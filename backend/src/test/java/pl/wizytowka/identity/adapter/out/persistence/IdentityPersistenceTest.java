package pl.wizytowka.identity.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import pl.wizytowka.identity.application.port.out.AdminUserRepository;
import pl.wizytowka.identity.application.port.out.RefreshTokenRepository;
import pl.wizytowka.identity.domain.AdminRole;
import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.RefreshToken;
import pl.wizytowka.identity.domain.TokenHashing;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.support.PersistenceTest;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PersistenceTest
class IdentityPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");

    @Autowired
    AdminUserRepository users;

    @Autowired
    RefreshTokenRepository tokens;

    @Autowired
    TestEntityManager em;

    @Test
    void savesAndRestoresAdminUserWithVersioning() {
        AdminUser user = AdminUser.create("anna@brand.pl", "Anna", AdminRole.EDITOR, "{pbkdf2}hash");
        users.save(user);
        em.flush();
        em.clear();

        AdminUser loaded = users.findByEmail("anna@brand.pl").orElseThrow();
        assertThat(loaded.id()).isEqualTo(user.id());
        assertThat(loaded.role()).isEqualTo(AdminRole.EDITOR);
        assertThat(loaded.version()).isZero();
        assertThat(users.countEnabledAdmins()).isZero();
        assertThat(users.existsAny()).isTrue();

        loaded.update("Anna K.", AdminRole.ADMIN);
        users.save(loaded);
        em.flush();
        em.clear();

        AdminUser updated = users.findById(user.id()).orElseThrow();
        assertThat(updated.version()).isEqualTo(1);
        assertThat(users.countEnabledAdmins()).isEqualTo(1);

        assertThatThrownBy(() -> users.save(loaded))
                .as("zapis obiektu ze starą wersją")
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void storesRefreshTokensAndRevokesByFamilyAndUser() {
        AdminUser user = AdminUser.create("anna@brand.pl", "Anna", AdminRole.ADMIN, "{pbkdf2}hash");
        users.save(user);
        em.flush();
        UUID family = UUID.randomUUID();
        RefreshToken first = RefreshToken.issue(user.id().value(), family, TokenHashing.sha256Hex("one"), NOW, Duration.ofDays(14));
        RefreshToken second = RefreshToken.issue(user.id().value(), family, TokenHashing.sha256Hex("two"), NOW, Duration.ofDays(14));
        RefreshToken other = RefreshToken.issue(user.id().value(), UUID.randomUUID(), TokenHashing.sha256Hex("three"), NOW, Duration.ofDays(14));
        tokens.save(first);
        tokens.save(second);
        tokens.save(other);
        em.flush();
        em.clear();

        RefreshToken loaded = tokens.findByHashForUpdate(TokenHashing.sha256Hex("one")).orElseThrow();
        loaded.markUsed(NOW);
        tokens.save(loaded);
        em.flush();
        em.clear();
        assertThat(tokens.findByHashForUpdate(TokenHashing.sha256Hex("one")).orElseThrow().usedAt()).isEqualTo(NOW);

        tokens.revokeFamily(family, NOW);
        em.clear();
        assertThat(tokens.findByHashForUpdate(TokenHashing.sha256Hex("two")).orElseThrow().isSpent()).isTrue();
        assertThat(tokens.findByHashForUpdate(TokenHashing.sha256Hex("three")).orElseThrow().isSpent()).isFalse();

        tokens.revokeAllOfUser(user.id(), NOW.plus(1, ChronoUnit.HOURS));
        em.clear();
        assertThat(tokens.findByHashForUpdate(TokenHashing.sha256Hex("three")).orElseThrow().revokedAt())
                .isEqualTo(NOW.plus(1, ChronoUnit.HOURS));
    }
}
