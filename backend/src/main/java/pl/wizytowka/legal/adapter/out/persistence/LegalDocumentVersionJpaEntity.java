package pl.wizytowka.legal.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UpdateTimestamp;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalDocumentVersion;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "legal_document_version")
class LegalDocumentVersionJpaEntity {

    @Id
    UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20, updatable = false)
    LegalDocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    LegalDocumentVersion.Status status;

    @Column(name = "version_number")
    Integer versionNumber;

    @Column(name = "acceptance_required", nullable = false)
    boolean acceptanceRequired;

    @Column(name = "published_at")
    Instant publishedAt;

    @Column(name = "published_by")
    UUID publishedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    /** Klucz: kod języka ({@code pl}, {@code en}…). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "legal_document_text", joinColumns = @JoinColumn(name = "version_id"))
    @MapKeyColumn(name = "language", length = 2)
    Map<String, Text> texts = new HashMap<>();

    @Version
    @Column(name = "version", nullable = false)
    Long version;

    protected LegalDocumentVersionJpaEntity() {
    }

    LegalDocumentVersionJpaEntity(UUID id) {
        this.id = id;
    }

    @Embeddable
    static class Text {

        @Column(name = "title", nullable = false, length = 200)
        String title;

        @Column(name = "body", nullable = false, columnDefinition = "text")
        String body;

        @Column(name = "change_summary", length = 1000)
        String changeSummary;

        protected Text() {
        }

        Text(String title, String body, String changeSummary) {
            this.title = title;
            this.body = body;
            this.changeSummary = changeSummary;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Text other && java.util.Objects.equals(title, other.title)
                    && java.util.Objects.equals(body, other.body)
                    && java.util.Objects.equals(changeSummary, other.changeSummary);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(title, body, changeSummary);
        }
    }
}
