package pl.wizytowka.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.identity.application.port.in.BuyerRegistrationUseCase;
import pl.wizytowka.identity.application.port.in.RegisterBuyerCommand;
import pl.wizytowka.identity.application.port.out.AdminUserRepository;
import pl.wizytowka.identity.application.port.out.BuyerAccountRepository;
import pl.wizytowka.identity.application.port.out.BuyerActionTokenRepository;
import pl.wizytowka.identity.application.port.out.BuyerMailer;
import pl.wizytowka.identity.application.port.out.BuyerRefreshTokenRepository;
import pl.wizytowka.identity.application.port.out.PasswordHasher;
import pl.wizytowka.identity.application.port.out.SecureTokenGenerator;
import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerActionPurpose;
import pl.wizytowka.identity.domain.BuyerActionToken;
import pl.wizytowka.identity.domain.BuyerStatus;
import pl.wizytowka.identity.domain.PasswordPolicy;
import pl.wizytowka.identity.domain.TaxId;
import pl.wizytowka.identity.domain.TokenHashing;
import pl.wizytowka.shared.application.port.out.TransactionalHooks;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Rejestracja kupca, potwierdzenie adresu i reset hasła. E-maile wychodzą dopiero po zatwierdzeniu transakcji
 * ({@link TransactionalHooks#afterCommit}), a odpowiedzi nie zależą od tego, czy konto o danym adresie istnieje.
 */
@Service
class BuyerRegistrationService implements BuyerRegistrationUseCase {

    static final String TOKEN_INVALID = "identity.buyer.token-invalid";
    private static final int MAX_RAW_TOKEN_LENGTH = 200;

    private final BuyerAccountRepository accounts;
    private final BuyerActionTokenRepository actionTokens;
    private final BuyerRefreshTokenRepository sessions;
    private final AdminUserRepository staff;
    private final PasswordHasher passwordHasher;
    private final SecureTokenGenerator tokenGenerator;
    private final BuyerMailer mailer;
    private final TransactionalHooks transactionalHooks;
    private final BuyerPolicy policy;
    private final Clock clock;

    BuyerRegistrationService(BuyerAccountRepository accounts, BuyerActionTokenRepository actionTokens,
                             BuyerRefreshTokenRepository sessions, AdminUserRepository staff,
                             PasswordHasher passwordHasher, SecureTokenGenerator tokenGenerator, BuyerMailer mailer,
                             TransactionalHooks transactionalHooks, BuyerPolicy policy, Clock clock) {
        this.accounts = accounts;
        this.actionTokens = actionTokens;
        this.sessions = sessions;
        this.staff = staff;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.mailer = mailer;
        this.transactionalHooks = transactionalHooks;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void register(RegisterBuyerCommand command) {
        Instant now = clock.instant();
        String email = BuyerAccount.normalizeEmail(command.email());
        TaxId taxId = TaxId.of(command.countryCode(), command.taxId());
        // hash liczony zawsze — czas odpowiedzi nie może zdradzać, czy adres jest już zajęty
        String passwordHash = passwordHasher.hash(PasswordPolicy.requireAcceptable(command.password()));
        BuyerAccount account = BuyerAccount.register(email, passwordHash, command.companyName(), taxId,
                command.contactName(), command.phone(), command.language(), command.termsAccepted(),
                command.privacyAccepted(), now);

        Optional<BuyerAccount> existing = accounts.findByEmail(email);
        if (existing.isPresent()) {
            BuyerAccount owner = existing.get();
            transactionalHooks.afterCommit(() -> mailer.sendAccountAlreadyExists(owner.email(), owner.language()));
            return;
        }
        accounts.save(account);
        String rawToken = issueToken(account, BuyerActionPurpose.EMAIL_VERIFICATION, policy.emailVerificationTtl(), now);
        transactionalHooks.afterCommit(() -> mailer.sendEmailVerification(account.email(), account.language(), rawToken));
    }

    @Override
    @Transactional
    public void verifyEmail(String rawToken) {
        Instant now = clock.instant();
        BuyerActionToken token = usableToken(rawToken, BuyerActionPurpose.EMAIL_VERIFICATION, now);
        BuyerAccount account = accounts.findById(token.buyerAccountId()).orElseThrow(BuyerRegistrationService::tokenInvalid);
        token.markUsed(now);
        actionTokens.save(token);
        if (account.verifyEmail(now)) {
            accounts.save(account);
            notifyStaff();
        }
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        Instant now = clock.instant();
        findByEmail(email)
                .filter(account -> account.status() == BuyerStatus.PENDING_EMAIL)
                .filter(account -> !inCooldown(account, BuyerActionPurpose.EMAIL_VERIFICATION, now))
                .ifPresent(account -> {
                    String rawToken = issueToken(account, BuyerActionPurpose.EMAIL_VERIFICATION,
                            policy.emailVerificationTtl(), now);
                    transactionalHooks.afterCommit(() ->
                            mailer.sendEmailVerification(account.email(), account.language(), rawToken));
                });
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        Instant now = clock.instant();
        findByEmail(email)
                .filter(account -> !inCooldown(account, BuyerActionPurpose.PASSWORD_RESET, now))
                .ifPresent(account -> {
                    String rawToken = issueToken(account, BuyerActionPurpose.PASSWORD_RESET, policy.passwordResetTtl(), now);
                    transactionalHooks.afterCommit(() ->
                            mailer.sendPasswordReset(account.email(), account.language(), rawToken));
                });
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Instant now = clock.instant();
        BuyerActionToken token = usableToken(rawToken, BuyerActionPurpose.PASSWORD_RESET, now);
        String passwordHash = passwordHasher.hash(PasswordPolicy.requireAcceptable(newPassword));
        BuyerAccount account = accounts.findById(token.buyerAccountId()).orElseThrow(BuyerRegistrationService::tokenInvalid);
        token.markUsed(now);
        actionTokens.save(token);
        account.changePasswordHash(passwordHash);
        // link z e-maila dowodzi też posiadania skrzynki — niepotwierdzony adres staje się potwierdzony
        boolean verifiedNow = account.verifyEmail(now);
        accounts.save(account);
        sessions.revokeAllOfBuyer(account.id(), now);
        if (verifiedNow) {
            notifyStaff();
        }
    }

    private String issueToken(BuyerAccount account, BuyerActionPurpose purpose, Duration ttl, Instant now) {
        actionTokens.invalidateUnused(account.id(), purpose, now);
        String rawToken = tokenGenerator.generate();
        actionTokens.save(BuyerActionToken.issue(account.id(), purpose, TokenHashing.sha256Hex(rawToken), now, ttl));
        return rawToken;
    }

    private boolean inCooldown(BuyerAccount account, BuyerActionPurpose purpose, Instant now) {
        return actionTokens.lastIssuedAt(account.id(), purpose)
                .map(issuedAt -> issuedAt.plus(BuyerActionPurpose.RESEND_COOLDOWN).isAfter(now))
                .orElse(false);
    }

    private BuyerActionToken usableToken(String rawToken, BuyerActionPurpose purpose, Instant now) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > MAX_RAW_TOKEN_LENGTH) {
            throw tokenInvalid();
        }
        return actionTokens.findByHashForUpdate(TokenHashing.sha256Hex(rawToken))
                .filter(token -> token.isUsableFor(purpose, now))
                .orElseThrow(BuyerRegistrationService::tokenInvalid);
    }

    private Optional<BuyerAccount> findByEmail(String email) {
        try {
            return accounts.findByEmail(BuyerAccount.normalizeEmail(email));
        } catch (InvalidValueException e) {
            return Optional.empty();
        }
    }

    private void notifyStaff() {
        List<String> recipients = staff.findAll().stream().filter(AdminUser::isEnabled).map(AdminUser::email).toList();
        if (!recipients.isEmpty()) {
            transactionalHooks.afterCommit(() -> mailer.notifyStaffAboutPendingBuyer(recipients));
        }
    }

    private static InvalidValueException tokenInvalid() {
        return new InvalidValueException(TOKEN_INVALID, "The link is invalid or has expired");
    }
}
