package pl.wizytowka.contact.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.wizytowka.shared.domain.InvalidValueException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalesRepresentativeDetailsTest {

    @Test
    void normalizesEmailAndBlankPhone() {
        var details = new SalesRepresentativeDetails("  Anna Nowak ", "Anna.Nowak@Brand.PL", "  ", true, 0);

        assertThat(details.fullName()).isEqualTo("Anna Nowak");
        assertThat(details.email()).isEqualTo("anna.nowak@brand.pl");
        assertThat(details.phone()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"+48 600 100 200", "(22) 123-45-67", "600100200"})
    void acceptsCommonPhoneFormats(String phone) {
        assertThat(new SalesRepresentativeDetails("A", "a@b.pl", phone, true, 0).phone()).isEqualTo(phone);
    }

    @ParameterizedTest
    @ValueSource(strings = {"call me", "+48 abc", "1234", "<script>"})
    void rejectsInvalidPhone(String phone) {
        assertThatThrownBy(() -> new SalesRepresentativeDetails("A", "a@b.pl", phone, true, 0))
                .isInstanceOf(InvalidValueException.class)
                .hasFieldOrPropertyWithValue("code", "contact.representative.phone");
    }

    @Test
    void rejectsInvalidEmailAndNegativeSortOrder() {
        assertThatThrownBy(() -> new SalesRepresentativeDetails("A", "not-an-email", null, true, 0))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new SalesRepresentativeDetails("A", "a@b.pl", null, true, -1))
                .isInstanceOf(InvalidValueException.class);
    }
}
