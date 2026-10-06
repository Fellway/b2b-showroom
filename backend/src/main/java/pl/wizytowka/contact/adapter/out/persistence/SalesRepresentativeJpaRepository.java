package pl.wizytowka.contact.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SalesRepresentativeJpaRepository extends JpaRepository<SalesRepresentativeJpaEntity, UUID> {

    List<SalesRepresentativeJpaEntity> findAllByOrderBySortOrderAscFullNameAsc();

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
