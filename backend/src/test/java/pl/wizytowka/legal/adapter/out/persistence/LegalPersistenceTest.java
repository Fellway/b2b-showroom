package pl.wizytowka.legal.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import pl.wizytowka.identity.application.port.out.BuyerAccountRepository;
import pl.wizytowka.identity.application.port.out.BuyerConsentRepository;
import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerConsent;
import pl.wizytowka.identity.domain.TaxId;
import pl.wizytowka.legal.application.port.out.LegalDocumentRepository;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalDocumentVersion;
import pl.wizytowka.legal.domain.LegalText;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.support.PersistenceTest;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Wersje dokumentów (V4) i rejestr zgód kupców na prawdziwym schemacie. */
@PersistenceTest
class LegalPersistenceTest {

    static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");

    @Autowired
    LegalDocumentRepository documents;

    @Autowired
    BuyerAccountRepository accounts;

    @Autowired
    BuyerConsentRepository consents;

    @Autowired
    TestEntityManager em;

    static Map<CatalogLanguage, LegalText> texts(String body) {
        Map<CatalogLanguage, LegalText> texts = new EnumMap<>(CatalogLanguage.class);
        for (CatalogLanguage language : CatalogLanguage.values()) {
            texts.put(language, new LegalText("Regulamin " + language.code(), body, null));
        }
        return texts;
    }

    private void flush() {
        em.flush();
        em.clear();
    }

    @Test
    void draftIsEditedThenPublishedAndBecomesImmutable() {
        LegalDocumentVersion draft = LegalDocumentVersion.draft(LegalDocumentType.TERMS,
                Map.of(CatalogLanguage.PL, new LegalText("Regulamin", "Szkic", null)), NOW);
        documents.save(draft);
        flush();

        LegalDocumentVersion loaded = documents.findDraft(LegalDocumentType.TERMS).orElseThrow();
        assertThat(loaded.texts()).containsOnlyKeys(CatalogLanguage.PL);
        loaded.updateTexts(texts("Treść v1"));
        documents.save(loaded);
        flush();

        LegalDocumentVersion edited = documents.findById(draft.id()).orElseThrow();
        assertThat(edited.version()).isEqualTo(1);
        assertThat(edited.texts()).hasSize(4);
        assertThatThrownBy(() -> documents.save(loaded)).as("nieaktualna wersja szkicu")
                .isInstanceOf(ConcurrentModificationException.class);

        edited.publish(1, true, null, NOW);
        documents.save(edited);
        flush();

        LegalDocumentVersion published = documents.findLatestPublished(LegalDocumentType.TERMS).orElseThrow();
        assertThat(published.versionNumber()).isEqualTo(1);
        assertThat(published.acceptanceRequired()).isTrue();
        assertThat(published.text(CatalogLanguage.UK)).get().extracting(LegalText::body).isEqualTo("Treść v1");
        assertThat(documents.findDraft(LegalDocumentType.TERMS)).isEmpty();
        assertThat(documents.existsAny(LegalDocumentType.PRIVACY_POLICY)).isFalse();
        assertThatThrownBy(() -> documents.save(published)).as("opublikowana treść jest niezmienna")
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void consentRegistryAnswersWhetherRequiredVersionWasAccepted() {
        LegalDocumentVersion v1 = LegalDocumentVersion.draft(LegalDocumentType.TERMS, texts("v1"), NOW);
        v1.publish(1, true, null, NOW);
        documents.save(v1);
        LegalDocumentVersion v2 = LegalDocumentVersion.draft(LegalDocumentType.TERMS, texts("v2"), NOW);
        v2.publish(2, true, null, NOW.plusSeconds(60));
        documents.save(v2);
        BuyerAccount buyer = BuyerAccount.register("jan@sklep.pl", "{pbkdf2}hash", "Sklep", TaxId.of("PL", "1234563218"),
                "Jan Kowalski", "+48 600 100 200", CatalogLanguage.PL, true, true, NOW);
        accounts.save(buyer);
        consents.save(BuyerConsent.record(buyer.id(), LegalDocumentType.TERMS, v1.id().value(), 1, CatalogLanguage.PL,
                BuyerConsent.Context.REGISTRATION, NOW));
        flush();

        assertThat(consents.hasAccepted(buyer.id(), LegalDocumentType.TERMS, 1)).isTrue();
        assertThat(consents.hasAccepted(buyer.id(), LegalDocumentType.TERMS, 2)).isFalse();
        assertThat(consents.hasAccepted(buyer.id(), LegalDocumentType.PRIVACY_POLICY, 1)).isFalse();

        consents.save(BuyerConsent.record(buyer.id(), LegalDocumentType.TERMS, v2.id().value(), 2, CatalogLanguage.EN,
                BuyerConsent.Context.UPDATE, NOW.plusSeconds(120)));
        flush();
        assertThat(consents.findByBuyer(buyer.id())).extracting(BuyerConsent::versionNumber).containsExactly(2, 1);

        accounts.delete(buyer.id());
        flush();
        assertThat(consents.findByBuyer(buyer.id())).as("rejestr znika razem z kontem").isEmpty();
    }
}
