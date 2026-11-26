package pl.wizytowka.legal.adapter.in.bootstrap;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalText;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Wzory dokumentów istnieją we wszystkich językach, mają tytuł i są oznaczone jako wymagające weryfikacji prawnej. */
class LegalTemplatesInitializerTest {

    @ParameterizedTest
    @EnumSource(LegalDocumentType.class)
    void everyLanguageHasACompleteTemplate(LegalDocumentType type) {
        Map<CatalogLanguage, LegalText> texts = LegalTemplatesInitializer.load(type, "Fabian's Style");

        assertThat(texts).containsOnlyKeys(CatalogLanguage.values());
        texts.forEach((language, text) -> {
            assertThat(text.title()).as(language + " title").isNotBlank().doesNotStartWith("#");
            assertThat(text.body()).as(language + " body")
                    .startsWith("> **")              // uwaga o weryfikacji prawnej na początku
                    .contains("[")                   // pola do uzupełnienia
                    .doesNotContain("{brand}")
                    .contains("## 1.");
        });
        assertThat(texts.get(CatalogLanguage.PL).body()).contains("Fabian's Style");
    }
}
