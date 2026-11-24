package pl.wizytowka.identity.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.wizytowka.identity.domain.AdminRole;
import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.AdminUserId;
import pl.wizytowka.identity.domain.RefreshToken;
import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.ResourceConflictException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminUserManagementServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");

    private final IdentityFakes.Users users = new IdentityFakes.Users();
    private final IdentityFakes.Tokens tokens = new IdentityFakes.Tokens();
    private AdminUserManagementService service;
    private AdminUser admin;

    @BeforeEach
    void setUp() {
        service = new AdminUserManagementService(users, tokens, new IdentityFakes.Hasher(), Clock.fixed(NOW, ZoneOffset.UTC));
        admin = IdentityFakes.user("admin@brand.pl", AdminRole.ADMIN, "irrelevant-password");
        users.save(admin);
    }

    @Test
    void createsUserWithHashedPasswordAndRejectsDuplicateEmail() {
        AdminUserId id = service.create("Editor@Brand.pl", "Ewa", AdminRole.EDITOR, "long enough password");

        AdminUser created = users.findById(id).orElseThrow();
        assertThat(created.email()).isEqualTo("editor@brand.pl");
        assertThat(created.passwordHash()).isEqualTo("{fake}long enough password");
        assertThatThrownBy(() -> service.create("editor@brand.pl", "X", AdminRole.EDITOR, "long enough password"))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void rejectsWeakPassword() {
        assertThatThrownBy(() -> service.create("e@brand.pl", "E", AdminRole.EDITOR, "short"))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void cannotDisableOrDemoteSelfNorLastAdmin() {
        assertThatThrownBy(() -> service.setEnabled(admin.id(), admin.id(), false))
                .isInstanceOf(BusinessRuleViolationException.class).extracting("code").isEqualTo("identity.cannot-disable-self");
        assertThatThrownBy(() -> service.update(admin.id(), admin.id(), 0, "A", AdminRole.EDITOR))
                .isInstanceOf(BusinessRuleViolationException.class).extracting("code").isEqualTo("identity.cannot-demote-self");

        AdminUserId otherActor = AdminUserId.newId();
        assertThatThrownBy(() -> service.setEnabled(otherActor, admin.id(), false))
                .isInstanceOf(BusinessRuleViolationException.class).extracting("code").isEqualTo("identity.last-admin");
    }

    @Test
    void disablingAnotherAdminRevokesSessions() {
        AdminUserId secondId = service.create("second@brand.pl", "Second", AdminRole.ADMIN, "long enough password");
        tokens.save(RefreshToken.issue(secondId.value(), UUID.randomUUID(), "a".repeat(64), NOW, Duration.ofDays(1)));

        service.setEnabled(admin.id(), secondId, false);

        assertThat(users.findById(secondId).orElseThrow().isEnabled()).isFalse();
        assertThat(tokens.activeCount()).isZero();
    }

    @Test
    void updateChecksExpectedVersion() {
        assertThatThrownBy(() -> service.update(admin.id(), admin.id(), 7, "A", AdminRole.ADMIN))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void resetPasswordUnlocksAndRevokesSessions() {
        tokens.save(RefreshToken.issue(admin.id().value(), UUID.randomUUID(), "b".repeat(64), NOW, Duration.ofDays(1)));

        service.resetPassword(admin.id(), "brand new password");

        assertThat(admin.passwordHash()).isEqualTo("{fake}brand new password");
        assertThat(tokens.activeCount()).isZero();
    }

    @Test
    void bootstrapCreatesAdminOnlyWhenNoneExists() {
        assertThat(service.createFirstAdminIfNoneExists("x@brand.pl", "long enough password")).isFalse();

        users.store.clear();
        assertThat(service.createFirstAdminIfNoneExists("x@brand.pl", "long enough password")).isTrue();
        assertThat(users.findByEmail("x@brand.pl").orElseThrow().role()).isEqualTo(AdminRole.ADMIN);
    }
}
