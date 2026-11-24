package pl.wizytowka.identity.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.wizytowka.shared.domain.InvalidValueException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxIdTest {

    @ParameterizedTest
    @ValueSource(strings = {"1234563218", "123-456-32-18", "PL 123 456 32 18", "pl1234563218", "123.456.32.18"})
    void normalizesPolishNip(String raw) {
        TaxId taxId = TaxId.of("pl", raw);

        assertThat(taxId.countryCode()).isEqualTo("PL");
        assertThat(taxId.number()).isEqualTo("1234563218");
        assertThat(taxId.display()).isEqualTo("PL 1234563218");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234563219", "123456321", "12345632180", "ABCDEFGHIJ", ""})
    void rejectsPolishNipWithWrongChecksumOrFormat(String raw) {
        assertThatThrownBy(() -> TaxId.of("PL", raw))
                .isInstanceOf(InvalidValueException.class)
                .hasFieldOrPropertyWithValue("code", "identity.buyer.tax-id");
    }

    @Test
    void validatesCzechDicFormatAndStripsPrefix() {
        assertThat(TaxId.of("CZ", "CZ12345678").number()).isEqualTo("12345678");
        assertThat(TaxId.of("cz", "1234567890").number()).isEqualTo("1234567890");
        assertThatThrownBy(() -> TaxId.of("CZ", "1234567")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void acceptsGenericVatNumbersForOtherCountriesIncludingGreekElPrefix() {
        assertThat(TaxId.of("DE", "DE 123 456 789").number()).isEqualTo("123456789");
        assertThat(TaxId.of("UA", "12345678").number()).isEqualTo("12345678");
        assertThat(TaxId.of("GR", "EL123456789").number()).isEqualTo("123456789");
        assertThatThrownBy(() -> TaxId.of("DE", "<script>")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void rejectsUnknownCountry() {
        assertThatThrownBy(() -> TaxId.of("XX", "123456"))
                .isInstanceOf(InvalidValueException.class)
                .hasFieldOrPropertyWithValue("code", "identity.buyer.country");
        assertThatThrownBy(() -> TaxId.of(null, "123456")).isInstanceOf(InvalidValueException.class);
    }
}
