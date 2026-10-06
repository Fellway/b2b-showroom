package pl.wizytowka.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.identity.application.port.in.AdminUserView;
import pl.wizytowka.identity.application.port.in.BootstrapAdminUseCase;
import pl.wizytowka.identity.application.port.in.ManageAdminUsersUseCase;
import pl.wizytowka.identity.application.port.out.AdminUserRepository;
import pl.wizytowka.identity.application.port.out.PasswordHasher;
import pl.wizytowka.identity.application.port.out.RefreshTokenRepository;
import pl.wizytowka.identity.domain.AdminRole;
import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.AdminUserId;
import pl.wizytowka.identity.domain.PasswordPolicy;
import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.shared.domain.ResourceConflictException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.time.Clock;
import java.util.List;

@Service
class AdminUserManagementService implements ManageAdminUsersUseCase, BootstrapAdminUseCase {

    private final AdminUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    AdminUserManagementService(AdminUserRepository users, RefreshTokenRepository refreshTokens,
                               PasswordHasher passwordHasher, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserView> list() {
        return users.findAll().stream().map(AdminUserView::of).toList();
    }

    @Override
    @Transactional
    public AdminUserId create(String email, String displayName, AdminRole role, String initialPassword) {
        String normalizedEmail = AdminUser.normalizeEmail(email);
        if (users.existsByEmail(normalizedEmail)) {
            throw new ResourceConflictException("identity.email-taken", "An admin with this e-mail already exists");
        }
        String hash = passwordHasher.hash(PasswordPolicy.requireAcceptable(initialPassword));
        AdminUser user = AdminUser.create(normalizedEmail, displayName, role, hash);
        users.save(user);
        return user.id();
    }

    @Override
    @Transactional
    public void update(AdminUserId actor, AdminUserId id, long expectedVersion, String displayName, AdminRole role) {
        AdminUser user = load(id);
        if (user.version() != expectedVersion) {
            throw new ConcurrentModificationException("identity.admin.concurrent-modification",
                    "Admin user was modified by someone else");
        }
        if (user.role() == AdminRole.ADMIN && role != AdminRole.ADMIN) {
            if (user.id().equals(actor)) {
                throw new BusinessRuleViolationException("identity.cannot-demote-self", "You cannot remove your own ADMIN role");
            }
            requireAnotherEnabledAdmin(user);
        }
        user.update(displayName, role);
        users.save(user);
    }

    @Override
    @Transactional
    public void setEnabled(AdminUserId actor, AdminUserId id, boolean enabled) {
        AdminUser user = load(id);
        if (enabled) {
            user.enable();
            users.save(user);
            return;
        }
        if (user.id().equals(actor)) {
            throw new BusinessRuleViolationException("identity.cannot-disable-self", "You cannot disable your own account");
        }
        requireAnotherEnabledAdmin(user);
        user.disable();
        users.save(user);
        refreshTokens.revokeAllOfUser(user.id(), clock.instant());
    }

    @Override
    @Transactional
    public void resetPassword(AdminUserId id, String newPassword) {
        AdminUser user = load(id);
        user.changePasswordHash(passwordHasher.hash(PasswordPolicy.requireAcceptable(newPassword)));
        users.save(user);
        refreshTokens.revokeAllOfUser(user.id(), clock.instant());
    }

    @Override
    @Transactional
    public boolean createFirstAdminIfNoneExists(String email, String password) {
        if (users.existsAny()) {
            return false;
        }
        create(email, "Administrator", AdminRole.ADMIN, password);
        return true;
    }

    private void requireAnotherEnabledAdmin(AdminUser user) {
        if (user.isEnabledAdmin() && users.countEnabledAdmins() <= 1) {
            throw new BusinessRuleViolationException("identity.last-admin",
                    "At least one enabled ADMIN account must remain");
        }
    }

    private AdminUser load(AdminUserId id) {
        return users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("identity.admin.not-found", "Admin user not found"));
    }
}
