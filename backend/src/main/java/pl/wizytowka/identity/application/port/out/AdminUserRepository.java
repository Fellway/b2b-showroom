package pl.wizytowka.identity.application.port.out;

import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.AdminUserId;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository {

    Optional<AdminUser> findById(AdminUserId id);

    Optional<AdminUser> findByEmail(String normalizedEmail);

    /** Posortowane po e-mailu. */
    List<AdminUser> findAll();

    /** @throws pl.wizytowka.shared.domain.ConcurrentModificationException przy niezgodności wersji */
    void save(AdminUser user);

    boolean existsByEmail(String normalizedEmail);

    boolean existsAny();

    long countEnabledAdmins();
}
