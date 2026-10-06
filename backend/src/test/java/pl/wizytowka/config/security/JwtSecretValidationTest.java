package pl.wizytowka.config.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class JwtSecretValidationTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"dG9vLXNob3J0", "not base64 !!!"})
    void failsFastOnMissingWeakOrMalformedSecret(String secret) {
        JwtProperties properties = new JwtProperties(secret, "issuer", Duration.ofMinutes(15), Duration.ofDays(14), true);

        assertThatIllegalStateException()
                .isThrownBy(() -> new SecurityConfig().jwtSigningKey(properties))
                .withMessageContaining("SHOWROOM_JWT_SECRET");
    }
}
