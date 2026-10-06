package pl.wizytowka.analytics.application.port.in;

import pl.wizytowka.analytics.domain.AnalyticsEventType;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.Map;
import java.util.UUID;

/** Zdarzenie rejestrowane przez inne moduły aplikacji (np. wygenerowanie zestawienia). */
public record ServerEvent(AnalyticsEventType type,
                          UUID visitorId,
                          CatalogLanguage language,
                          UUID productId,
                          UUID variantId,
                          UUID lineSheetId,
                          Map<String, String> properties) {
}
