package pl.wizytowka.linesheet.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "line_sheet")
class LineSheetJpaEntity {

    @Id
    UUID id;

    @Column(name = "public_id", nullable = false, updatable = false)
    UUID publicId;

    @Column(name = "reference_number", nullable = false, length = 24, updatable = false)
    String referenceNumber;

    @Column(name = "locale", nullable = false, length = 2, updatable = false)
    CatalogLanguage language;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    String currency;

    @Column(name = "company_name", length = 200, updatable = false)
    String companyName;

    @Column(name = "contact_name", length = 120, updatable = false)
    String contactName;

    @Column(name = "email", length = 254, updatable = false)
    String email;

    @Column(name = "phone", length = 32, updatable = false)
    String phone;

    @Column(name = "tax_id", length = 32, updatable = false)
    String taxId;

    @Column(name = "message", length = 2000, updatable = false)
    String message;

    @Column(name = "privacy_accepted_at", updatable = false)
    Instant privacyAcceptedAt;

    @Column(name = "sales_representative_id", updatable = false)
    UUID salesRepresentativeId;

    @Column(name = "visitor_id", updatable = false)
    UUID visitorId;

    @Column(name = "pdf_storage_key", length = 512)
    String pdfStorageKey;

    @Column(name = "pdf_generated_at")
    Instant pdfGeneratedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    Long version;

    @OneToMany(mappedBy = "lineSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    List<LineSheetItemJpaEntity> items = new ArrayList<>();

    protected LineSheetJpaEntity() {
    }

    LineSheetJpaEntity(UUID id) {
        this.id = id;
    }
}
