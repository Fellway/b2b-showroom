package pl.wizytowka.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.identity.application.port.in.AccessToken;
import pl.wizytowka.identity.application.port.in.AdminUserView;
import pl.wizytowka.identity.application.port.in.AuthenticateUseCase;
import pl.wizytowka.identity.application.port.in.AuthenticationFailedException;
import pl.wizytowka.identity.application.port.in.AuthenticationResult;
import pl.wizytowka.identity.application.port.in.CurrentAdminQuery;
import pl.wizytowka.identity.application.port.out.AccessTokenIssuer;
import pl.wizytowka.identity.application.port.out.AdminUserRepository;
import pl.wizytowka.identity.application.port.out.PasswordHasher;
import pl.wizytowka.identity.application.port.out.RefreshTokenRepository;
import pl.wizytowka.identity.application.port.out.SecureTokenGenerator;
import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.AdminUserId;
import pl.wizytowka.identity.domain.RefreshToken;
import pl.wizytowka.identity.domain.TokenHashing;
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
 * Logowanie i rotacja sesji. Nieudane próby są zapisywane mimo zgłoszenia wyjątku
 * ({@code noRollbackFor}), inaczej blokada konta i unieważnienie skradzionej rodziny tokenów nie przetrwałyby.
 */
@Service
class AuthenticationService implements AuthenticateUseCase, CurrentAdminQuery {

    private static final int MAX_RAW_TOKEN_LENGTH = 200;

    private final AdminUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final SecureTokenGenerator tokenGenerator;
    private final IdentityPolicy policy;
    private final Clock clock;
    /** Hash porównywany przy nieznanym e-mailu, by czas odpowiedzi nie zdradzał istnienia konta. */
    private final String dummyPasswordHash;

    AuthenticationService(AdminUserRepository users, RefreshTokenRepository refreshTokens, PasswordHasher passwordHasher,
                          AccessTokenIssuer accessTokenIssuer, SecureTokenGenerator tokenGenerator,
                          IdentityPolicy policy, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.tokenGenerator = tokenGenerator;
        this.policy = policy;
        this.clock = clock;
        this.dummyPasswordHash = passwordHasher.hash(tokenGenerator.generate());
    }

    @Override
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthenticationResult login(String email, String password) {
        Instant now = clock.instant();
        String rawPassword = password == null ? "" : password;
        Optional<AdminUser> found = normalizedEmail(email).flatMap(users::findByEmail);
        if (found.isEmpty()) {
            passwordHasher.matches(rawPassword, dummyPasswordHash);
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        AdminUser user = found.get();
        boolean passwordMatches = passwordHasher.matches(rawPassword, user.passwordHash());
        if (!user.canLogIn(now)) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        if (!passwordMatches) {
            user.recordFailedLogin(now, policy.lockout());
            users.save(user);
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        user.recordSuccessfulLogin(now);
        users.save(user);
        return issueSession(user, Uuids.v7(), now);
    }

    @Override
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthenticationResult refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken token = findToken(rawRefreshToken)
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_REFRESH_TOKEN));
        if (token.isSpent()) {
            // ponowne użycie zużytego tokenu: ktoś inny ma kopię — zamykamy całą sesję
            refreshTokens.revokeFamily(token.familyId(), now);
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }
        if (token.isExpired(now)) {
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }
        AdminUser user = users.findById(token.adminUserId())
                .filter(AdminUser::isEnabled)
                .orElse(null);
        if (user == null) {
            refreshTokens.revokeFamily(token.familyId(), now);
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }
        token.markUsed(now);
        refreshTokens.save(token);
        return issueSession(user, token.familyId(), now);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        findToken(rawRefreshToken).ifPresent(token -> refreshTokens.revokeFamily(token.familyId(), clock.instant()));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserView me(AdminUserId adminUserId) {
        return users.findById(adminUserId)
                .map(AdminUserView::of)
                .orElseThrow(() -> new ResourceNotFoundException("identity.admin.not-found", "Admin user not found"));
    }

    private AuthenticationResult issueSession(AdminUser user, UUID familyId, Instant now) {
        String rawRefreshToken = tokenGenerator.generate();
        RefreshToken refreshToken = RefreshToken.issue(user.id(), familyId, TokenHashing.sha256Hex(rawRefreshToken),
                now, policy.refreshTokenTtl());
        refreshTokens.save(refreshToken);
        AccessToken accessToken = accessTokenIssuer.issue(user, now);
        return new AuthenticationResult(accessToken, rawRefreshToken, refreshToken.expiresAt(), AdminUserView.of(user));
    }

    private Optional<RefreshToken> findToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank() || rawRefreshToken.length() > MAX_RAW_TOKEN_LENGTH) {
            return Optional.empty();
        }
        return refreshTokens.findByHashForUpdate(TokenHashing.sha256Hex(rawRefreshToken));
    }

    private static Optional<String> normalizedEmail(String email) {
        try {
            return Optional.of(AdminUser.normalizeEmail(email));
        } catch (InvalidValueException e) {
            return Optional.empty();
        }
    }
}
