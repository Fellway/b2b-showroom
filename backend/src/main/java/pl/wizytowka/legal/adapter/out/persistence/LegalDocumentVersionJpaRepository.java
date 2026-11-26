package pl.wizytowka.legal.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalDocumentVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LegalDocumentVersionJpaRepository extends JpaRepository<LegalDocumentVersionJpaEntity, UUID> {

    List<LegalDocumentVersionJpaEntity> findByDocumentTypeAndStatusOrderByVersionNumberDesc(
            LegalDocumentType documentType, LegalDocumentVersion.Status status);

    Optional<LegalDocumentVersionJpaEntity> findFirstByDocumentTypeAndStatusOrderByVersionNumberDesc(
            LegalDocumentType documentType, LegalDocumentVersion.Status status);

    boolean existsByDocumentType(LegalDocumentType documentType);
}
