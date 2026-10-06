package pl.wizytowka.analytics.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.analytics.application.port.in.RecordAnalyticsEventsUseCase;
import pl.wizytowka.shared.adapter.in.web.RequestedLanguage;
import pl.wizytowka.shared.domain.CatalogLanguage;

@RestController
@RequestMapping("/api/v1/analytics")
class AnalyticsEventsController {

    private final RecordAnalyticsEventsUseCase recordEvents;

    AnalyticsEventsController(RecordAnalyticsEventsUseCase recordEvents) {
        this.recordEvents = recordEvents;
    }

    /** Przyjmuje paczkę zdarzeń z przeglądarki (np. wysyłaną przez navigator.sendBeacon). */
    @PostMapping("/events")
    ResponseEntity<Void> record(@Valid @RequestBody AnalyticsEventsRequest request,
                                @RequestedLanguage CatalogLanguage language) {
        recordEvents.recordClientEvents(request.events().stream().map(e -> e.toClientEvent(language)).toList());
        return ResponseEntity.accepted().build();
    }
}
