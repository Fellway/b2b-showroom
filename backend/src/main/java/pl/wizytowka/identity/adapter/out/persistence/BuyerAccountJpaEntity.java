package pl.wizytowka.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UpdateTimestamp;
import pl.wizytowka.identity.domain.BuyerStatus;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "buyer_account")
class BuyerAccountJpaEntity {

    @Id
    UUID id;

    @Column(name = "email", nullable = false, length = 254, updatable = false)
    String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    String passwordHash;

    @Column(name = "company_name", nullable = false, length = 200)
    String companyName;

    @Column(name = "country_code", nullable = false, length = 2)
    String countryCode;

    @Column(name = "tax_id", nullable = false, length = 32)
    String taxId;

    @Column(name = "contact_name", nullable = false, length = 120)
    String contactName;

    @Column(name = "phone", nullable = false, length = 32)
    String phone;

    @Column(name = "language", nullable = false, length = 2)
    CatalogLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    BuyerStatus status;

    @Column(name = "email_verified_at")
    Instant emailVerifiedAt;

    @Column(name = "reviewed_at")
    Instant reviewedAt;

    @Column(name = "reviewed_by")
    UUID reviewedBy;

    @Column(name = "review_note", length = 500)
    String reviewNote;

    @Column(name = "failed_login_attempts", nullable = false)
    int failedLoginAttempts;

    @Column(name = "locked_until")
    Instant lockedUntil;

    @Column(name = "last_login_at")
    Instant lastLoginAt;

    /** Czas rejestracji z domeny (część historii zgód), nie znacznik techniczny. */
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    /** Ustawiany przez Hibernate przy każdym zapisie, także przy INSERT. */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    Long version;

    protected BuyerAccountJpaEntity() {
    }

    BuyerAccountJpaEntity(UUID id) {
        this.id = id;
    }
}
