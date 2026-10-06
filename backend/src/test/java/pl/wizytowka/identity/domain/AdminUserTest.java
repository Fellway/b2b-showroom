package pl.wizytowka.identity.domain;

import org.junit.jupiter.api.Test;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminUserTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private static final LockoutPolicy POLICY = new LockoutPolicy(5, Duration.ofMinutes(15));

    @Test
    void normalizesEmailAndRejectsInvalidOne() {
        AdminUser user = AdminUser.create("  Anna@Brand.PL ", "Anna", AdminRole.EDITOR, "hash");

        assertThat(user.email()).isEqualTo("anna@brand.pl");
        assertThatThrownBy(() -> AdminUser.create("not-an-email", "X", AdminRole.EDITOR, "hash"))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void locksAfterMaxFailedAttemptsAndUnlocksAfterDuration() {
        AdminUser user = AdminUser.create("a@b.pl", "A", AdminRole.ADMIN, "hash");
        for (int i = 0; i < 4; i++) {
            user.recordFailedLogin(NOW, POLICY);
        }
        assertThat(user.isLocked(NOW)).isFalse();
        assertThat(user.failedLoginAttempts()).isEqualTo(4);

        user.recordFailedLogin(NOW, POLICY);

        assertThat(user.isLocked(NOW)).isTrue();
        assertThat(user.canLogIn(NOW.plus(Duration.ofMinutes(14)))).isFalse();
        assertThat(user.canLogIn(NOW.plus(Duration.ofMinutes(15)))).isTrue();
    }

    @Test
    void failedAttemptsWhileLockedDoNotExtendLock() {
        AdminUser user = AdminUser.create("a@b.pl", "A", AdminRole.ADMIN, "hash");
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(NOW, POLICY);
        }
        Instant lockedUntil = user.lockedUntil();

        user.recordFailedLogin(NOW.plusSeconds(60), POLICY);

        assertThat(user.lockedUntil()).isEqualTo(lockedUntil);
    }

    @Test
    void successfulLoginAndPasswordChangeResetCounters() {
        AdminUser user = AdminUser.create("a@b.pl", "A", AdminRole.ADMIN, "hash");
        user.recordFailedLogin(NOW, POLICY);
        user.recordSuccessfulLogin(NOW);
        assertThat(user.failedLoginAttempts()).isZero();
        assertThat(user.lastLoginAt()).isEqualTo(NOW);

        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(NOW, POLICY);
        }
        user.changePasswordHash("new-hash");
        assertThat(user.isLocked(NOW)).isFalse();
        assertThat(user.passwordHash()).isEqualTo("new-hash");
    }

    @Test
    void disabledUserCannotLogIn() {
        AdminUser user = AdminUser.create("a@b.pl", "A", AdminRole.ADMIN, "hash");
        user.disable();

        assertThat(user.canLogIn(NOW)).isFalse();
        assertThat(user.isEnabledAdmin()).isFalse();
    }
}
