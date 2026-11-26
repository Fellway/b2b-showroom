package pl.wizytowka.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.identity.application.port.in.AccessToken;
import pl.wizytowka.identity.application.port.in.AccountNotActiveException;
import pl.wizytowka.identity.application.port.in.AuthenticationFailedException;
import pl.wizytowka.identity.application.port.in.BuyerAccountView;
import pl.wizytowka.identity.application.port.in.BuyerAuthenticationUseCase;
import pl.wizytowka.identity.application.port.in.BuyerSelfServiceUseCase;
import pl.wizytowka.identity.application.port.in.BuyerSession;
import pl.wizytowka.identity.application.port.out.AccessTokenIssuer;
import pl.wizytowka.identity.application.port.out.BuyerAccountRepository;
import pl.wizytowka.identity.application.port.out.BuyerMailer;
import pl.wizytowka.identity.application.port.out.BuyerRefreshTokenRepository;
import pl.wizytowka.identity.application.port.out.PasswordHasher;
import pl.wizytowka.identity.application.port.out.SecureTokenGenerator;
import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.PasswordPolicy;
import pl.wizytowka.identity.domain.RefreshToken;
import pl.wizytowka.identity.domain.TokenHashing;
import pl.wizytowka.shared.application.port.out.TransactionalHooks;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;
import pl.wizytowka.shared.domain.Uuids;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static pl.wizytowka.identity.application.port.in.AuthenticationFailedException.INVALID_CREDENTIALS;
import static pl.wizytowka.identity.application.port.in.AuthenticationFailedException.INVALID_REFRESH_TOKEN;

/**
 * Logowanie kupca i operacje na własnym koncie. Ta sama mechanika co sesja panelu ({@link AuthenticationService}):
 * nieudane próby są zapisywane mimo wyjątku ({@code noRollbackFor}), żeby blokada konta przetrwała.
 */
@Service
class BuyerAuthenticationService implements BuyerAuthenticationUseCase, BuyerSelfServiceUseCase {

    static final String CURRENT_PASSWORD_INVALID = "identity.buyer.current-password-invalid";
    private static final int MAX_RAW_TOKEN_LENGTH = 200;

    private final BuyerAccountRepository accounts;
    private final BuyerRefreshTokenRepository sessions;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final SecureTokenGenerator tokenGenerator;
    private final BuyerPolicy policy;
    private final BuyerLegalService legal;
    private final BuyerAccountRemoval removal;
    private final BuyerMailer mailer;
    private final TransactionalHooks transactionalHooks;
    private final Clock clock;
    /** Hash porównywany przy nieznanym e-mailu, by czas odpowiedzi nie zdradzał istnienia konta. */
    private final String dummyPasswordHash;

    BuyerAuthenticationService(BuyerAccountRepository accounts, BuyerRefreshTokenRepository sessions,
                               PasswordHasher passwordHasher, AccessTokenIssuer accessTokenIssuer,
                               SecureTokenGenerator tokenGenerator, BuyerPolicy policy, BuyerLegalService legal,
                               BuyerAccountRemoval removal, BuyerMailer mailer, TransactionalHooks transactionalHooks,
                               Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.tokenGenerator = tokenGenerator;
        this.policy = policy;
        this.legal = legal;
        this.removal = removal;
        this.mailer = mailer;
        this.transactionalHooks = transactionalHooks;
        this.clock = clock;
        this.dummyPasswordHash = passwordHasher.hash(tokenGenerator.generate());
    }

    // ------------------------------------------------------------------ sesja

    @Override
    @Transactional(noRollbackFor = {AuthenticationFailedException.class, AccountNotActiveException.class})
    public BuyerSession login(String email, String password) {
        Instant now = clock.instant();
        String rawPassword = password == null ? "" : password;
        Optional<BuyerAccount> found = normalizedEmail(email).flatMap(accounts::findByEmail);
        if (found.isEmpty()) {
            passwordHasher.matches(rawPassword, dummyPasswordHash);
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        BuyerAccount account = found.get();
        boolean passwordMatches = passwordHasher.matches(rawPassword, account.passwordHash());
        if (account.isLocked(now)) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        if (!passwordMatches) {
            account.recordFailedLogin(now, policy.lockout());
            accounts.save(account);
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        if (!account.isActive()) {
            // stan ujawniamy dopiero osobie, która zna hasło
            throw new AccountNotActiveException(account.status());
        }
        account.recordSuccessfulLogin(now);
        accounts.save(account);
        return issueSession(account, Uuids.v7(), now);
    }

    @Override
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public BuyerSession refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken token = findToken(rawRefreshToken)
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_REFRESH_TOKEN));
        if (token.isSpent()) {
            // ponowne użycie zużytego tokenu: ktoś inny ma kopię — zamykamy całą sesję
            sessions.revokeFamily(token.familyId(), now);
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }
        if (token.isExpired(now)) {
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }
        BuyerAccount account = accounts.findById(BuyerAccountId.of(token.subjectId()))
                .filter(BuyerAccount::isActive)
                .orElse(null);
        if (account == null) {
            sessions.revokeFamily(token.familyId(), now);
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }
        token.markUsed(now);
        sessions.save(token);
        return issueSession(account, token.familyId(), now);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        findToken(rawRefreshToken).ifPresent(token -> sessions.revokeFamily(token.familyId(), clock.instant()));
    }

    // ------------------------------------------------------------------ własne konto

    @Override
    @Transactional(readOnly = true)
    public BuyerAccountView me(BuyerAccountId id) {
        return BuyerAccountView.of(load(id));
    }

    @Override
    @Transactional
    public void updateProfile(BuyerAccountId id, long expectedVersion, String contactName, String phone,
                              CatalogLanguage language) {
        BuyerAccount account = load(id);
        requireVersion(account, expectedVersion);
        account.updateProfile(contactName, phone, language);
        accounts.save(account);
    }

    @Override
    @Transactional(noRollbackFor = InvalidValueException.class)
    public BuyerSession changePassword(BuyerAccountId id, String currentPassword, String newPassword) {
        Instant now = clock.instant();
        BuyerAccount account = load(id);
        String newHash = passwordHasher.hash(PasswordPolicy.requireAcceptable(newPassword));
        boolean currentMatches = currentPassword != null && passwordHasher.matches(currentPassword, account.passwordHash());
        if (!currentMatches || account.isLocked(now)) {
            // ta sama ochrona przed zgadywaniem co przy logowaniu
            account.recordFailedLogin(now, policy.lockout());
            accounts.save(account);
            throw new InvalidValueException(CURRENT_PASSWORD_INVALID, "Current password is incorrect");
        }
        account.changePasswordHash(newHash);
        accounts.save(account);
        sessions.revokeAllOfBuyer(account.id(), now);
        return issueSession(account, Uuids.v7(), now);
    }

    @Override
    @Transactional(readOnly = true)
    public BuyerDataExport export(BuyerAccountId id) {
        BuyerAccount account = load(id);
        return new BuyerDataExport(BuyerAccountView.of(account), account.emailVerifiedAt(), account.reviewedAt(),
                account.lastLoginAt(), legal.history(id));
    }

    @Override
    @Transactional(noRollbackFor = InvalidValueException.class)
    public void deleteAccount(BuyerAccountId id, String currentPassword) {
        Instant now = clock.instant();
        BuyerAccount account = load(id);
        boolean currentMatches = currentPassword != null && passwordHasher.matches(currentPassword, account.passwordHash());
        if (!currentMatches || account.isLocked(now)) {
            account.recordFailedLogin(now, policy.lockout());
            accounts.save(account);
            throw new InvalidValueException(CURRENT_PASSWORD_INVALID, "Current password is incorrect");
        }
        String email = account.email();
        CatalogLanguage language = account.language();
        removal.remove(account);
        transactionalHooks.afterCommit(() -> mailer.sendAccountDeleted(email, language));
    }

    // ------------------------------------------------------------------ pomocnicze

    private BuyerSession issueSession(BuyerAccount account, UUID familyId, Instant now) {
        String rawRefreshToken = tokenGenerator.generate();
        RefreshToken refreshToken = RefreshToken.issue(account.id().value(), familyId,
                TokenHashing.sha256Hex(rawRefreshToken), now, policy.refreshTokenTtl());
        sessions.save(refreshToken);
        AccessToken accessToken = accessTokenIssuer.issue(account, now);
        return new BuyerSession(accessToken, rawRefreshToken, refreshToken.expiresAt(), BuyerAccountView.of(account));
    }

    private Optional<RefreshToken> findToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank() || rawRefreshToken.length() > MAX_RAW_TOKEN_LENGTH) {
            return Optional.empty();
        }
        return sessions.findByHashForUpdate(TokenHashing.sha256Hex(rawRefreshToken));
    }

    private BuyerAccount load(BuyerAccountId id) {
        return accounts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("identity.buyer.not-found", "Buyer account not found"));
    }

    static void requireVersion(BuyerAccount account, long expectedVersion) {
        if (account.version() != expectedVersion) {
            throw new ConcurrentModificationException("identity.buyer.concurrent-modification",
                    "Buyer account was modified by someone else");
        }
    }

    private static Optional<String> normalizedEmail(String email) {
        try {
            return Optional.of(BuyerAccount.normalizeEmail(email));
        } catch (InvalidValueException e) {
            return Optional.empty();
        }
    }
}
