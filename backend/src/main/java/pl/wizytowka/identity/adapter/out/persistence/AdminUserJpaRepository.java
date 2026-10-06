package pl.wizytowka.identity.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.wizytowka.identity.domain.AdminRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AdminUserJpaRepository extends JpaRepository<AdminUserJpaEntity, UUID> {

    Optional<AdminUserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRoleAndEnabledTrue(AdminRole role);

    List<AdminUserJpaEntity> findAllByOrderByEmailAsc();
}
