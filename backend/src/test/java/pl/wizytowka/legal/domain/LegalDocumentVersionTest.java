package pl.wizytowka.legal.domain;

import org.junit.jupiter.api.Test;
import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegalDocumentVersionTest {

    static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");

    static Map<CatalogLanguage, LegalText> allLanguages() {
        Map<CatalogLanguage, LegalText> texts = new EnumMap<>(CatalogLanguage.class);
        for (CatalogLanguage language : CatalogLanguage.values()) {
            texts.put(language, new LegalText("Title " + language.code(), "Body " + language.code(), null));
        }
        return texts;
    }

    @Test
    void draftWithMissingTranslationCannotBePublished() {
        LegalDocumentVersion draft = LegalDocumentVersion.draft(LegalDocumentType.TERMS,
                Map.of(CatalogLanguage.PL, new LegalText("Regulamin", "Treść", null)), NOW);

        assertThat(draft.missingLanguages()).containsExactly(CatalogLanguage.EN, CatalogLanguage.CS, CatalogLanguage.UK);
        assertThatThrownBy(() -> draft.publish(1, true, null, NOW))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("code", "legal.document.translation-missing");
        assertThat(draft.text(CatalogLanguage.UK)).as("brak tłumaczenia → język bazowy").get()
                .extracting(LegalText::title).isEqualTo("Regulamin");
        assertThat(draft.resolvedLanguage(CatalogLanguage.UK)).isEqualTo(CatalogLanguage.PL);
    }

    @Test
    void publishedVersionIsImmutable() {
        LegalDocumentVersion version = LegalDocumentVersion.draft(LegalDocumentType.TERMS, allLanguages(), NOW);
        UUID actor = UUID.randomUUID();

        version.publish(3, true, actor, NOW);

        assertThat(version.isPublished()).isTrue();
        assertThat(version.versionNumber()).isEqualTo(3);
        assertThat(version.publishedBy()).isEqualTo(actor);
        assertThatThrownBy(() -> version.updateTexts(allLanguages()))
                .hasFieldOrPropertyWithValue("code", "legal.document.published-immutable");
        assertThatThrownBy(() -> version.publish(4, true, actor, NOW))
                .hasFieldOrPropertyWithValue("code", "legal.document.published-immutable");
    }

    @Test
    void onlyTermsCanRequireAcceptanceAndTheFirstVersionAlwaysDoes() {
        LegalDocumentVersion firstTerms = LegalDocumentVersion.draft(LegalDocumentType.TERMS, allLanguages(), NOW);
        firstTerms.publish(1, false, null, NOW);
        assertThat(firstTerms.acceptanceRequired()).isTrue();

        LegalDocumentVersion editorialFix = LegalDocumentVersion.draft(LegalDocumentType.TERMS, allLanguages(), NOW);
        editorialFix.publish(2, false, null, NOW);
        assertThat(editorialFix.acceptanceRequired()).isFalse();

        LegalDocumentVersion privacy = LegalDocumentVersion.draft(LegalDocumentType.PRIVACY_POLICY, allLanguages(), NOW);
        privacy.publish(1, true, null, NOW);
        assertThat(privacy.acceptanceRequired()).as("polityka prywatności to informacja, nie zgoda").isFalse();
    }

    @Test
    void textRequiresTitleAndBodyWithinLimits() {
        assertThatThrownBy(() -> new LegalText(" ", "Treść", null)).isInstanceOf(InvalidValueException.class)
                .hasFieldOrPropertyWithValue("code", "legal.text.title");
        assertThatThrownBy(() -> new LegalText("Tytuł", "x".repeat(LegalText.MAX_BODY + 1), null))
                .hasFieldOrPropertyWithValue("code", "legal.text.body");
        assertThat(new LegalText(" Tytuł ", "Treść", "  ").changeSummary()).isNull();
        assertThat(LegalDocumentType.fromSlug("privacy")).contains(LegalDocumentType.PRIVACY_POLICY);
        assertThat(LegalDocumentType.fromSlug("cookies")).isEmpty();
    }
}
