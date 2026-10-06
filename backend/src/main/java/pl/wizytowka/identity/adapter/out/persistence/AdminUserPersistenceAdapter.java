package pl.wizytowka.identity.adapter.out.persistence;

import org.springframework.stereotype.Component;
import pl.wizytowka.identity.application.port.out.AdminUserRepository;
import pl.wizytowka.identity.domain.AdminRole;
import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.AdminUserId;
import pl.wizytowka.shared.domain.ConcurrentModificationException;

import java.util.List;
import java.util.Optional;

@Component
class AdminUserPersistenceAdapter implements AdminUserRepository {

    private final AdminUserJpaRepository repository;

    AdminUserPersistenceAdapter(AdminUserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AdminUser> findById(AdminUserId id) {
        return repository.findById(id.value()).map(AdminUserPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<AdminUser> findByEmail(String normalizedEmail) {
        return repository.findByEmail(normalizedEmail).map(AdminUserPersistenceAdapter::toDomain);
    }

    @Override
    public List<AdminUser> findAll() {
        return repository.findAllByOrderByEmailAsc().stream().map(AdminUserPersistenceAdapter::toDomain).toList();
    }

    @Override
    public void save(AdminUser user) {
        AdminUserJpaEntity entity = repository.findById(user.id().value())
                .map(existing -> {
                    if (existing.getVersion() != user.version()) {
                        throw new ConcurrentModificationException("identity.admin.concurrent-modification",
                                "Admin user was modified by someone else");
                    }
                    return existing;
                })
                .orElseGet(() -> new AdminUserJpaEntity(user.id().value()));
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setDisplayName(user.displayName());
        entity.setRole(user.role());
        entity.setEnabled(user.isEnabled());
        entity.setFailedLoginAttempts(user.failedLoginAttempts());
        entity.setLockedUntil(user.lockedUntil());
        entity.setLastLoginAt(user.lastLoginAt());
        repository.save(entity);
    }

    @Override
    public boolean existsByEmail(String normalizedEmail) {
        return repository.existsByEmail(normalizedEmail);
    }

    @Override
    public boolean existsAny() {
        return repository.count() > 0;
    }

    @Override
    public long countEnabledAdmins() {
        return repository.countByRoleAndEnabledTrue(AdminRole.ADMIN);
    }

    private static AdminUser toDomain(AdminUserJpaEntity e) {
        return AdminUser.restore(AdminUserId.of(e.getId()), e.getEmail(), e.getPasswordHash(), e.getDisplayName(),
                e.getRole(), e.isEnabled(), e.getFailedLoginAttempts(), e.getLockedUntil(), e.getLastLoginAt(),
                e.getVersion());
    }
}
