package pl.wizytowka.identity.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.wizytowka.identity.domain.BuyerStatus;

import java.util.Optional;
import java.util.UUID;

interface BuyerAccountJpaRepository extends JpaRepository<BuyerAccountJpaEntity, UUID> {

    Optional<BuyerAccountJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByStatus(BuyerStatus status);

    String FILTER = """
            from BuyerAccountJpaEntity a
            where (:status is null or a.status = :status)
              and (:pattern is null
                   or lower(a.email) like :pattern escape '!'
                   or lower(a.companyName) like :pattern escape '!'
                   or lower(a.contactName) like :pattern escape '!'
                   or lower(a.taxId) like :pattern escape '!')
            """;

    @Query(value = "select a " + FILTER + " order by a.createdAt desc, a.id desc",
            countQuery = "select count(a) " + FILTER)
    Page<BuyerAccountJpaEntity> find(BuyerStatus status, String pattern, Pageable pageable);
}
