package pl.wizytowka.catalog.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.wizytowka.shared.domain.InvalidValueException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GtinTest {

    @ParameterizedTest
    @ValueSource(strings = {"5901234123457", "4006381333931", "96385074", "036000291452", "10012345678902", " 5901234123457 "})
    void acceptsValidGtins(String value) {
        assertThat(Gtin.of(value).value()).isEqualTo(value.strip());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5901234123458", "123", "59012341234570", "590123412345A", ""})
    void rejectsInvalidGtins(String value) {
        assertThatThrownBy(() -> Gtin.of(value)).isInstanceOf(InvalidValueException.class);
        assertThat(Gtin.isValid(value)).isFalse();
    }
}
