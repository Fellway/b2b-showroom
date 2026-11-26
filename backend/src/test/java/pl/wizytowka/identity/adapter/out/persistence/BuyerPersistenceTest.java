package pl.wizytowka.identity.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import pl.wizytowka.identity.application.port.in.BuyerFilter;
import pl.wizytowka.identity.application.port.out.BuyerAccountRepository;
import pl.wizytowka.identity.application.port.out.BuyerActionTokenRepository;
import pl.wizytowka.identity.application.port.out.BuyerRefreshTokenRepository;
import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerActionPurpose;
import pl.wizytowka.identity.domain.BuyerActionToken;
import pl.wizytowka.identity.domain.BuyerStatus;
import pl.wizytowka.identity.domain.RefreshToken;
import pl.wizytowka.identity.domain.TaxId;
import pl.wizytowka.identity.domain.TokenHashing;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.support.PersistenceTest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PersistenceTest
class BuyerPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-11-24T10:00:00Z");

    @Autowired
    BuyerAccountRepository accounts;

    @Autowired
    BuyerRefreshTokenRepository sessions;

    @Autowired
    BuyerActionTokenRepository actionTokens;

    @Autowired
    TestEntityManager em;

    private BuyerAccount register(String email, String company, String nip) {
        BuyerAccount account = BuyerAccount.register(email, "{pbkdf2}hash", company, TaxId.of("PL", nip), "Jan Kowalski",
                "+48 600 100 200", CatalogLanguage.PL, true, true, NOW);
        accounts.save(account);
        em.flush();
        em.clear();
        return account;
    }

    @Test
    void savesRestoresAndVersionsBuyerAccount() {
        BuyerAccount account = register("jan@sklep.pl", "Sklep Jan", "1234563218");

        BuyerAccount loaded = accounts.findByEmail("jan@sklep.pl").orElseThrow();
        assertThat(loaded.id()).isEqualTo(account.id());
        assertThat(loaded.taxId()).isEqualTo(TaxId.of("PL", "1234563218"));
        assertThat(loaded.status()).isEqualTo(BuyerStatus.PENDING_EMAIL);
        assertThat(loaded.language()).isEqualTo(CatalogLanguage.PL);
        assertThat(loaded.createdAt()).isEqualTo(NOW);
        assertThat(loaded.version()).isZero();
        assertThat(accounts.existsByEmail("jan@sklep.pl")).isTrue();

        loaded.verifyEmail(NOW);
        accounts.save(loaded);
        em.flush();
        em.clear();

        BuyerAccount verified = accounts.findById(account.id()).orElseThrow();
        assertThat(verified.status()).isEqualTo(BuyerStatus.PENDING_APPROVAL);
        assertThat(verified.version()).isEqualTo(1);
        assertThat(accounts.countByStatus(BuyerStatus.PENDING_APPROVAL)).isEqualTo(1);

        assertThatThrownBy(() -> accounts.save(loaded)).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void filtersByStatusAndSearchWithEscapedPattern() {
        BuyerAccount first = register("anna@moda.pl", "Moda 100% Anna", "1234563218");
        register("piotr@butik.pl", "Butik Piotr", "5260250274");
        BuyerAccount verified = accounts.findById(first.id()).orElseThrow();
        verified.verifyEmail(NOW);
        accounts.save(verified);
        em.flush();
        em.clear();

        assertThat(accounts.find(BuyerFilter.all(), new PageQuery(0, 10)).totalItems()).isEqualTo(2);
        assertThat(accounts.find(new BuyerFilter(BuyerStatus.PENDING_APPROVAL, null), new PageQuery(0, 10)).items())
                .extracting(BuyerAccount::email).containsExactly("anna@moda.pl");
        assertThat(accounts.find(new BuyerFilter(null, "BUTIK"), new PageQuery(0, 10)).items())
                .extracting(BuyerAccount::email).containsExactly("piotr@butik.pl");
        assertThat(accounts.find(new BuyerFilter(null, "526025"), new PageQuery(0, 10)).items())
                .as("wyszukiwanie po numerze podatkowym").hasSize(1);
        PageResult<BuyerAccount> percent = accounts.find(new BuyerFilter(null, "100%"), new PageQuery(0, 10));
        assertThat(percent.items()).extracting(BuyerAccount::email).containsExactly("anna@moda.pl");
        assertThat(accounts.find(new BuyerFilter(null, "%"), new PageQuery(0, 10)).items())
                .as("'%' wyszukuje dosłownie, nie jako wzorzec").hasSize(1);
    }

    @Test
    void sessionsAndActionTokensFollowTheAccount() {
        BuyerAccount account = register("jan@sklep.pl", "Sklep Jan", "1234563218");
        UUID family = UUID.randomUUID();
        sessions.save(RefreshToken.issue(account.id().value(), family, TokenHashing.sha256Hex("s1"), NOW, Duration.ofDays(30)));
        sessions.save(RefreshToken.issue(account.id().value(), UUID.randomUUID(), TokenHashing.sha256Hex("s2"), NOW,
                Duration.ofDays(30)));
        actionTokens.save(BuyerActionToken.issue(account.id(), BuyerActionPurpose.PASSWORD_RESET,
                TokenHashing.sha256Hex("r1"), NOW, Duration.ofHours(1)));
        em.flush();
        em.clear();

        assertThat(sessions.findByHashForUpdate(TokenHashing.sha256Hex("s1"))).get()
                .extracting(RefreshToken::subjectId).isEqualTo(account.id().value());
        sessions.revokeAllOfBuyer(account.id(), NOW);
        assertThat(sessions.findByHashForUpdate(TokenHashing.sha256Hex("s2")).orElseThrow().isSpent()).isTrue();

        assertThat(actionTokens.lastIssuedAt(account.id(), BuyerActionPurpose.PASSWORD_RESET)).contains(NOW);
        assertThat(actionTokens.lastIssuedAt(account.id(), BuyerActionPurpose.EMAIL_VERIFICATION)).isEmpty();
        actionTokens.invalidateUnused(account.id(), BuyerActionPurpose.PASSWORD_RESET, NOW);
        assertThat(actionTokens.findByHashForUpdate(TokenHashing.sha256Hex("r1")).orElseThrow()
                .isUsableFor(BuyerActionPurpose.PASSWORD_RESET, NOW)).isFalse();

        accounts.delete(account.id());
        em.flush();
        em.clear();
        assertThat(accounts.findById(account.id())).isEmpty();
        assertThat(sessions.findByHashForUpdate(TokenHashing.sha256Hex("s1"))).as("kaskada w bazie").isEmpty();
        assertThat(actionTokens.findByHashForUpdate(TokenHashing.sha256Hex("r1"))).isEmpty();
    }
}
