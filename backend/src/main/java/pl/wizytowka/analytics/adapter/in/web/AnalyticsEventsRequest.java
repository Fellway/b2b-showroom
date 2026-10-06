package pl.wizytowka.analytics.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.wizytowka.analytics.application.port.in.ClientEvent;
import pl.wizytowka.analytics.application.port.in.RecordAnalyticsEventsUseCase;
import pl.wizytowka.analytics.domain.AnalyticsEventType;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

record AnalyticsEventsRequest(@NotEmpty @Size(max = RecordAnalyticsEventsUseCase.MAX_BATCH_SIZE) List<@Valid @NotNull Event> events) {

    /** @param language kod ISO 639-1; brak = język żądania */
    record Event(@NotNull AnalyticsEventType type,
                 UUID visitorId,
                 UUID sessionId,
                 String language,
                 UUID productId,
                 UUID variantId,
                 @Size(max = 512) String path,
                 @Size(max = 255) String referrerHost,
                 @Size(max = 20) Map<String, @Size(max = 200) String> properties) {

        ClientEvent toClientEvent(CatalogLanguage requestLanguage) {
            CatalogLanguage eventLanguage = language == null ? requestLanguage
                    : CatalogLanguage.fromCode(language).orElse(requestLanguage);
            return new ClientEvent(type, visitorId, sessionId, eventLanguage, productId, variantId, path, referrerHost,
                    properties);
        }
    }
}
