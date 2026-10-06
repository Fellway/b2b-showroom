package pl.wizytowka.analytics.application.port.in;

import pl.wizytowka.analytics.domain.AnalyticsEventType;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.Map;
import java.util.UUID;

/** Zdarzenie zgłoszone przez przeglądarkę. Czas zdarzenia nadaje serwer. */
public record ClientEvent(AnalyticsEventType type,
                          UUID visitorId,
                          UUID sessionId,
                          CatalogLanguage language,
                          UUID productId,
                          UUID variantId,
                          String path,
                          String referrerHost,
                          Map<String, String> properties) {
}
