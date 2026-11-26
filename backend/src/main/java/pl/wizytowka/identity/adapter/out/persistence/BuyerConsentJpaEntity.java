package pl.wizytowka.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import pl.wizytowka.identity.domain.BuyerConsent;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.UUID;

/** Wpis rejestru zgód — tylko wstawiany ({@link Immutable}). */
@Entity
@Immutable
@Table(name = "buyer_consent")
class BuyerConsentJpaEntity {

    @Id
    UUID id;

    @Column(name = "buyer_account_id", nullable = false)
    UUID buyerAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    LegalDocumentType documentType;

    @Column(name = "document_version_id", nullable = false)
    UUID documentVersionId;

    @Column(name = "version_number", nullable = false)
    int versionNumber;

    @Column(name = "language", nullable = false, length = 2)
    CatalogLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(name = "context", nullable = false, length = 16)
    BuyerConsent.Context context;

    @Column(name = "accepted_at", nullable = false)
    Instant acceptedAt;

    protected BuyerConsentJpaEntity() {
    }
}
