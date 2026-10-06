package pl.wizytowka.analytics.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalyticsEventTest {

    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");

    @Test
    void normalizesReferrerHost() {
        AnalyticsEvent event = event("/pl/katalog", "WWW.Google.COM", Map.of("utm_source", "newsletter"));

        assertThat(event.referrerHost()).isEqualTo("www.google.com");
        assertThat(event.properties()).containsEntry("utm_source", "newsletter");
    }

    @ParameterizedTest
    @ValueSource(strings = {"katalog", "/a b", "/x\ny"})
    void rejectsInvalidPath(String path) {
        assertThatThrownBy(() -> event(path, null, Map.of())).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://google.com", "google.com/search?q=x", "user@host", "-bad.com"})
    void rejectsReferrerThatIsNotBareHost(String host) {
        assertThatThrownBy(() -> event("/", host, Map.of()))
                .hasFieldOrPropertyWithValue("code", "analytics.event.referrer-host");
    }

    @Test
    void limitsProperties() {
        Map<String, String> tooMany = new HashMap<>();
        IntStream.range(0, 21).forEach(i -> tooMany.put("k" + i, "v"));

        assertThatThrownBy(() -> event("/", null, tooMany)).hasFieldOrPropertyWithValue("code", "analytics.event.properties");
        assertThatThrownBy(() -> event("/", null, Map.of("Bad-Key", "v")))
                .hasFieldOrPropertyWithValue("code", "analytics.event.property-key");
        assertThatThrownBy(() -> event("/", null, Map.of("k", "v".repeat(201))))
                .hasFieldOrPropertyWithValue("code", "analytics.event.property-value");
    }

    private static AnalyticsEvent event(String path, String referrer, Map<String, String> properties) {
        return new AnalyticsEvent(AnalyticsEventType.PAGE_VIEW, NOW, null, null, null, null, null, null,
                path, referrer, properties);
    }
}
