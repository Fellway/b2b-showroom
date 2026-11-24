package pl.wizytowka.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pl.wizytowka.identity.domain.BuyerActionPurpose;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "buyer_action_token")
class BuyerActionTokenJpaEntity {

    @Id
    UUID id;

    @Column(name = "buyer_account_id", nullable = false, updatable = false)
    UUID buyerAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 24, updatable = false)
    BuyerActionPurpose purpose;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    Instant expiresAt;

    @Column(name = "used_at")
    Instant usedAt;

    protected BuyerActionTokenJpaEntity() {
    }

    BuyerActionTokenJpaEntity(UUID id) {
        this.id = id;
    }
}
