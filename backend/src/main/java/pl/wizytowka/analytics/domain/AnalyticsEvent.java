package pl.wizytowka.analytics.domain;

import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Pojedyncze zdarzenie analityczne (append-only, niezmienne). Celowo nie przechowuje adresu IP ani
 * User-Agenta (RODO): {@code visitorId}/{@code sessionId} to losowe identyfikatory nadane przez przeglądarkę
 * po wyrażeniu zgody, a {@code referrerHost} to sama nazwa hosta, bez ścieżki i parametrów.
 */
public record AnalyticsEvent(AnalyticsEventType type,
                             Instant occurredAt,
                             UUID visitorId,
                             UUID sessionId,
                             CatalogLanguage language,
                             UUID productId,
                             UUID variantId,
                             UUID lineSheetId,
                             String path,
                             String referrerHost,
                             Map<String, String> properties) {

    public static final int MAX_PROPERTIES = 20;
    private static final int MAX_PATH = 512;
    private static final int MAX_PROPERTY_VALUE = 200;
    private static final Pattern PATH = Pattern.compile("^/[^\\s\\p{Cntrl}]*$");
    private static final Pattern HOST = Pattern.compile("^(?=.{1,255}$)[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)*$");
    private static final Pattern PROPERTY_KEY = Pattern.compile("^[a-z0-9_]{1,40}$");

    public AnalyticsEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (path != null) {
            if (path.length() > MAX_PATH || !PATH.matcher(path).matches()) {
                throw new InvalidValueException("analytics.event.path", "Path must start with '/' and have at most 512 characters");
            }
        }
        if (referrerHost != null) {
            referrerHost = referrerHost.strip().toLowerCase(Locale.ROOT);
            if (!HOST.matcher(referrerHost).matches()) {
                throw new InvalidValueException("analytics.event.referrer-host", "Referrer must be a bare host name");
            }
        }
        properties = properties == null ? Map.of() : Map.copyOf(properties);
        if (properties.size() > MAX_PROPERTIES) {
            throw new InvalidValueException("analytics.event.properties", "At most " + MAX_PROPERTIES + " properties allowed");
        }
        properties.forEach((key, value) -> {
            if (!PROPERTY_KEY.matcher(key).matches()) {
                throw new InvalidValueException("analytics.event.property-key", "Property keys must match [a-z0-9_]{1,40}");
            }
            if (value.length() > MAX_PROPERTY_VALUE) {
                throw new InvalidValueException("analytics.event.property-value", "Property values must have at most 200 characters");
            }
        });
    }
}
