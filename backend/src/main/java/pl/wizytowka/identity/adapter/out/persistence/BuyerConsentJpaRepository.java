package pl.wizytowka.identity.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.wizytowka.legal.domain.LegalDocumentType;

import java.util.List;
import java.util.UUID;

interface BuyerConsentJpaRepository extends JpaRepository<BuyerConsentJpaEntity, UUID> {

    List<BuyerConsentJpaEntity> findByBuyerAccountIdOrderByAcceptedAtDescIdDesc(UUID buyerAccountId);

    @Query("""
            select count(c) > 0 from BuyerConsentJpaEntity c
            where c.buyerAccountId = :buyerAccountId and c.documentType = :type and c.versionNumber >= :minimumVersion
            """)
    boolean hasAccepted(UUID buyerAccountId, LegalDocumentType type, int minimumVersion);
}
