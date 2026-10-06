package pl.wizytowka.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Refresh token (przechowywany wyłącznie jako hash). Każde odświeżenie zużywa token i wystawia następny
 * w tej samej rodzinie ({@code familyId} = jedna sesja logowania). Ponowne użycie zużytego tokenu oznacza
 * jego kradzież — wtedy unieważniana jest cała rodzina.
 */
public final class RefreshToken {

    private final RefreshTokenId id;
    private final AdminUserId adminUserId;
    private final UUID familyId;
    private final String tokenHash;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private Instant usedAt;
    private Instant revokedAt;

    private RefreshToken(RefreshTokenId id, AdminUserId adminUserId, UUID familyId, String tokenHash, Instant issuedAt,
                         Instant expiresAt, Instant usedAt, Instant revokedAt) {
        this.id = Objects.requireNonNull(id);
        this.adminUserId = Objects.requireNonNull(adminUserId);
        this.familyId = Objects.requireNonNull(familyId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.issuedAt = Objects.requireNonNull(issuedAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("Refresh token must expire after issue time");
        }
        this.usedAt = usedAt;
        this.revokedAt = revokedAt;
    }

    public static RefreshToken issue(AdminUserId adminUserId, UUID familyId, String tokenHash, Instant now, Duration ttl) {
        return new RefreshToken(RefreshTokenId.newId(), adminUserId, familyId, tokenHash, now, now.plus(ttl), null, null);
    }

    public static RefreshToken restore(RefreshTokenId id, AdminUserId adminUserId, UUID familyId, String tokenHash,
                                       Instant issuedAt, Instant expiresAt, Instant usedAt, Instant revokedAt) {
        return new RefreshToken(id, adminUserId, familyId, tokenHash, issuedAt, expiresAt, usedAt, revokedAt);
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    /** Token już raz wymieniony lub odwołany — jego ponowne użycie to sygnał kradzieży. */
    public boolean isSpent() {
        return usedAt != null || revokedAt != null;
    }

    public void markUsed(Instant now) {
        if (isSpent()) {
            throw new IllegalStateException("Refresh token already spent");
        }
        this.usedAt = Objects.requireNonNull(now);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = Objects.requireNonNull(now);
        }
    }

    public RefreshTokenId id() {
        return id;
    }

    public AdminUserId adminUserId() {
        return adminUserId;
    }

    public UUID familyId() {
        return familyId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant usedAt() {
        return usedAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }
}
