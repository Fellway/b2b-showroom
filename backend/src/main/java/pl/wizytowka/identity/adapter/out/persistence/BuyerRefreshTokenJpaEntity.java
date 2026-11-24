package pl.wizytowka.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "buyer_refresh_token")
class BuyerRefreshTokenJpaEntity {

    @Id
    UUID id;

    @Column(name = "buyer_account_id", nullable = false, updatable = false)
    UUID buyerAccountId;

    @Column(name = "family_id", nullable = false, updatable = false)
    UUID familyId;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    Instant expiresAt;

    @Column(name = "used_at")
    Instant usedAt;

    @Column(name = "revoked_at")
    Instant revokedAt;

    protected BuyerRefreshTokenJpaEntity() {
    }

    BuyerRefreshTokenJpaEntity(UUID id, UUID buyerAccountId, UUID familyId, String tokenHash, Instant issuedAt,
                               Instant expiresAt) {
        this.id = id;
        this.buyerAccountId = buyerAccountId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }
}
