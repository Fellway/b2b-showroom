package pl.wizytowka.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");

    @Test
    void freshTokenIsUsableUntilExpiry() {
        RefreshToken token = issue();

        assertThat(token.isSpent()).isFalse();
        assertThat(token.isExpired(NOW.plus(Duration.ofDays(14)).minusSeconds(1))).isFalse();
        assertThat(token.isExpired(NOW.plus(Duration.ofDays(14)))).isTrue();
    }

    @Test
    void usedTokenCannotBeUsedAgain() {
        RefreshToken token = issue();
        token.markUsed(NOW);

        assertThat(token.isSpent()).isTrue();
        assertThatIllegalStateException().isThrownBy(() -> token.markUsed(NOW));
    }

    @Test
    void revokedTokenIsSpentAndRevocationIsIdempotent() {
        RefreshToken token = issue();
        token.revoke(NOW);
        token.revoke(NOW.plusSeconds(10));

        assertThat(token.isSpent()).isTrue();
        assertThat(token.revokedAt()).isEqualTo(NOW);
    }

    @Test
    void hashIsSha256Hex() {
        assertThat(TokenHashing.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    private static RefreshToken issue() {
        return RefreshToken.issue(AdminUserId.newId(), UUID.randomUUID(), TokenHashing.sha256Hex("t"), NOW, Duration.ofDays(14));
    }
}
