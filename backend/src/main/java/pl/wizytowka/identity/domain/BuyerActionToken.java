package pl.wizytowka.identity.domain;

import pl.wizytowka.shared.domain.Uuids;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Jednorazowy token z e-maila (potwierdzenie adresu, reset hasła). Przechowywany wyłącznie jako hash SHA-256;
 * surowa wartość istnieje tylko w wysłanej wiadomości. Wydanie nowego tokenu tego samego rodzaju unieważnia
 * poprzednie (zob. port repozytorium).
 */
public final class BuyerActionToken {

    private final UUID id;
    private final BuyerAccountId buyerAccountId;
    private final BuyerActionPurpose purpose;
    private final String tokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant usedAt;

    private BuyerActionToken(UUID id, BuyerAccountId buyerAccountId, BuyerActionPurpose purpose, String tokenHash,
                             Instant createdAt, Instant expiresAt, Instant usedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.buyerAccountId = Objects.requireNonNull(buyerAccountId, "buyerAccountId");
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Action token must expire after creation");
        }
        this.usedAt = usedAt;
    }

    public static BuyerActionToken issue(BuyerAccountId buyerAccountId, BuyerActionPurpose purpose, String tokenHash,
                                         Instant now, Duration ttl) {
        return new BuyerActionToken(Uuids.v7(), buyerAccountId, purpose, tokenHash, now, now.plus(ttl), null);
    }

    public static BuyerActionToken restore(UUID id, BuyerAccountId buyerAccountId, BuyerActionPurpose purpose,
                                           String tokenHash, Instant createdAt, Instant expiresAt, Instant usedAt) {
        return new BuyerActionToken(id, buyerAccountId, purpose, tokenHash, createdAt, expiresAt, usedAt);
    }

    /** Czy token można teraz wykorzystać do wskazanego celu. */
    public boolean isUsableFor(BuyerActionPurpose expected, Instant now) {
        return purpose == expected && usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(Instant now) {
        if (usedAt != null) {
            throw new IllegalStateException("Action token already used");
        }
        this.usedAt = Objects.requireNonNull(now);
    }

    public UUID id() {
        return id;
    }

    public BuyerAccountId buyerAccountId() {
        return buyerAccountId;
    }

    public BuyerActionPurpose purpose() {
        return purpose;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant usedAt() {
        return usedAt;
    }
}
