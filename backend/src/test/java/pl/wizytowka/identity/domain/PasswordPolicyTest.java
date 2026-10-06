package pl.wizytowka.identity.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pl.wizytowka.shared.domain.InvalidValueException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"correct horse", "dwanaście-zn", "ąęółśżźćńąęó"})
    void acceptsPasswordsOfAllowedLength(String password) {
        assertThat(PasswordPolicy.requireAcceptable(password)).isEqualTo(password);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short", "elevenchars", "            "})
    void rejectsTooShortOrBlank(String password) {
        assertThatThrownBy(() -> PasswordPolicy.requireAcceptable(password)).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {129, 1000})
    void rejectsTooLong(int length) {
        assertThatThrownBy(() -> PasswordPolicy.requireAcceptable("x".repeat(length)))
                .isInstanceOf(InvalidValueException.class);
    }
}
