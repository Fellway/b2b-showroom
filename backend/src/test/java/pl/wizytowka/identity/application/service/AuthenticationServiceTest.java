package pl.wizytowka.identity.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.wizytowka.identity.application.port.in.AuthenticationFailedException;
import pl.wizytowka.identity.application.port.in.AuthenticationResult;
import pl.wizytowka.identity.domain.AdminRole;
import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.LockoutPolicy;
import pl.wizytowka.identity.domain.TokenHashing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationServiceTest {

    private static final String PASSWORD = "correct horse battery";
    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");

    private final IdentityFakes.Users users = new IdentityFakes.Users();
    private final IdentityFakes.Tokens tokens = new IdentityFakes.Tokens();
    private final IdentityFakes.Hasher hasher = new IdentityFakes.Hasher();
    private AuthenticationService service;
    private AdminUser admin;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(users, tokens, hasher, new IdentityFakes.Issuer(),
                new IdentityFakes.SequentialTokens(),
                new IdentityPolicy(new LockoutPolicy(5, Duration.ofMinutes(15)), Duration.ofDays(14)),
                Clock.fixed(NOW, ZoneOffset.UTC));
        admin = IdentityFakes.user("admin@brand.pl", AdminRole.ADMIN, PASSWORD);
        users.save(admin);
    }

    @Test
    void loginIssuesAccessAndHashedRefreshToken() {
        AuthenticationResult result = service.login(" Admin@Brand.pl ", PASSWORD);

        assertThat(result.accessToken().value()).isEqualTo("access-admin@brand.pl");
        assertThat(result.refreshTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(14)));
        assertThat(tokens.byHash).containsKey(TokenHashing.sha256Hex(result.refreshToken()));
        assertThat(tokens.byHash).doesNotContainKey(result.refreshToken());
        assertThat(admin.lastLoginAt()).isEqualTo(NOW);
    }

    @Test
    void unknownEmailStillVerifiesAgainstDummyHash() {
        int callsBefore = hasher.matchCalls.get();

        assertThatThrownBy(() -> service.login("nobody@brand.pl", PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class)
                .extracting("code").isEqualTo(AuthenticationFailedException.INVALID_CREDENTIALS);
        assertThat(hasher.matchCalls.get()).isEqualTo(callsBefore + 1);
    }

    @Test
    void fiveWrongPasswordsLockTheAccountEvenForCorrectPassword() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.login("admin@brand.pl", "wrong password!"))
                    .isInstanceOf(AuthenticationFailedException.class);
        }

        assertThat(admin.isLocked(NOW)).isTrue();
        assertThatThrownBy(() -> service.login("admin@brand.pl", PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class)
                .extracting("code").isEqualTo(AuthenticationFailedException.INVALID_CREDENTIALS);
    }

    @Test
    void disabledUserCannotLogIn() {
        admin.disable();

        assertThatThrownBy(() -> service.login("admin@brand.pl", PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void refreshRotatesTokenWithinTheSameFamily() {
        AuthenticationResult first = service.login("admin@brand.pl", PASSWORD);

        AuthenticationResult second = service.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        var oldToken = tokens.byHash.get(TokenHashing.sha256Hex(first.refreshToken()));
        var newToken = tokens.byHash.get(TokenHashing.sha256Hex(second.refreshToken()));
        assertThat(oldToken.usedAt()).isEqualTo(NOW);
        assertThat(newToken.familyId()).isEqualTo(oldToken.familyId());
        assertThat(newToken.isSpent()).isFalse();
    }

    @Test
    void reusingSpentTokenRevokesWholeFamily() {
        AuthenticationResult first = service.login("admin@brand.pl", PASSWORD);
        AuthenticationResult second = service.refresh(first.refreshToken());

        assertThatThrownBy(() -> service.refresh(first.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class)
                .extracting("code").isEqualTo(AuthenticationFailedException.INVALID_REFRESH_TOKEN);
        assertThatThrownBy(() -> service.refresh(second.refreshToken()))
                .as("legalny następca też jest unieważniony")
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(tokens.activeCount()).isZero();
    }

    @Test
    void refreshFailsForUnknownMissingOrDisabledUserTokens() {
        assertThatThrownBy(() -> service.refresh(null)).isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> service.refresh("unknown")).isInstanceOf(AuthenticationFailedException.class);

        AuthenticationResult session = service.login("admin@brand.pl", PASSWORD);
        admin.disable();
        assertThatThrownBy(() -> service.refresh(session.refreshToken())).isInstanceOf(AuthenticationFailedException.class);
        assertThat(tokens.activeCount()).isZero();
    }

    @Test
    void logoutRevokesFamilyAndIgnoresUnknownToken() {
        AuthenticationResult session = service.login("admin@brand.pl", PASSWORD);

        service.logout("unknown");
        service.logout(session.refreshToken());

        assertThat(tokens.activeCount()).isZero();
        assertThatThrownBy(() -> service.refresh(session.refreshToken())).isInstanceOf(AuthenticationFailedException.class);
    }
}
