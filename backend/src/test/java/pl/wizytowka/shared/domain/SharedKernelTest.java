package pl.wizytowka.shared.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedKernelTest {

    @Test
    void uuidV7IsVersion7AndTimeOrdered() throws InterruptedException {
        UUID first = Uuids.v7();
        Thread.sleep(2);
        UUID second = Uuids.v7();

        assertThat(first.version()).isEqualTo(7);
        assertThat(first.variant()).isEqualTo(2);
        assertThat(first.toString()).isLessThan(second.toString());
    }

    @Test
    void translationsFallBackToDefaultLanguage() {
        Translations<String> names = Translations.of(Map.of(CatalogLanguage.PL, "Granatowy", CatalogLanguage.EN, "Navy"));

        assertThat(names.resolve(CatalogLanguage.EN)).isEqualTo("Navy");
        assertThat(names.resolve(CatalogLanguage.UK)).isEqualTo("Granatowy");
        assertThat(names.exact(CatalogLanguage.UK)).isEmpty();
    }

    @Test
    void translationsRequireDefaultLanguage() {
        assertThatThrownBy(() -> Translations.of(Map.of(CatalogLanguage.EN, "Navy")))
                .isInstanceOf(InvalidValueException.class)
                .extracting(e -> ((DomainException) e).code()).isEqualTo("i18n.default-language-required");
    }

    @Test
    void pricingNormalizesScaleAndComputesMarkup() {
        Pricing pricing = Pricing.pln("149", "349.99");

        assertThat(pricing.wholesalePrice()).isEqualByComparingTo("149.00");
        assertThat(pricing.wholesalePrice().scale()).isEqualTo(2);
        assertThat(pricing.markupMultiplier()).isEqualByComparingTo(new BigDecimal("2.35"));
    }

    @Test
    void pricingRejectsInvalidValues() {
        assertThatThrownBy(() -> Pricing.pln("0", "10")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Pricing.pln("10.001", "20")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Pricing.pln("10000000000", "20")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void languageCodesAreCaseInsensitive() {
        assertThat(CatalogLanguage.fromCode(" UK ")).contains(CatalogLanguage.UK);
        assertThat(CatalogLanguage.fromCode("de")).isEmpty();
    }
}
