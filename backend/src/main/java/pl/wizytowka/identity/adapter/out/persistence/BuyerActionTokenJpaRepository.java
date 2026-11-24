package pl.wizytowka.identity.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pl.wizytowka.identity.domain.BuyerActionPurpose;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface BuyerActionTokenJpaRepository extends JpaRepository<BuyerActionTokenJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from BuyerActionTokenJpaEntity t where t.tokenHash = :tokenHash")
    Optional<BuyerActionTokenJpaEntity> findByTokenHashForUpdate(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update BuyerActionTokenJpaEntity t set t.usedAt = :now
            where t.buyerAccountId = :buyerAccountId and t.purpose = :purpose and t.usedAt is null
            """)
    int invalidateUnused(UUID buyerAccountId, BuyerActionPurpose purpose, Instant now);

    @Query("""
            select max(t.createdAt) from BuyerActionTokenJpaEntity t
            where t.buyerAccountId = :buyerAccountId and t.purpose = :purpose
            """)
    Optional<Instant> lastIssuedAt(UUID buyerAccountId, BuyerActionPurpose purpose);
}
