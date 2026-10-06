package pl.wizytowka.analytics.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.analytics.application.port.in.ClientEvent;
import pl.wizytowka.analytics.application.port.in.RecordAnalyticsEventsUseCase;
import pl.wizytowka.analytics.application.port.in.ServerEvent;
import pl.wizytowka.analytics.application.port.out.AnalyticsEventStore;
import pl.wizytowka.analytics.domain.AnalyticsEvent;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
class AnalyticsRecordingService implements RecordAnalyticsEventsUseCase {

    private final AnalyticsEventStore store;
    private final Clock clock;

    AnalyticsRecordingService(AnalyticsEventStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void recordClientEvents(List<ClientEvent> events) {
        if (events == null || events.isEmpty() || events.size() > MAX_BATCH_SIZE) {
            throw new InvalidValueException("analytics.batch.size", "Batch must contain 1-" + MAX_BATCH_SIZE + " events");
        }
        Instant now = clock.instant();
        List<AnalyticsEvent> accepted = events.stream().map(event -> {
            if (event.type() == null || !event.type().isClientReportable()) {
                throw new InvalidValueException("analytics.event.type-not-allowed",
                        "Event type cannot be reported by the client: " + event.type());
            }
            return new AnalyticsEvent(event.type(), now, event.visitorId(), event.sessionId(), event.language(),
                    event.productId(), event.variantId(), null, event.path(), event.referrerHost(), event.properties());
        }).toList();
        store.append(accepted);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordServerEvent(ServerEvent event) {
        store.append(List.of(new AnalyticsEvent(event.type(), clock.instant(), event.visitorId(), null,
                event.language(), event.productId(), event.variantId(), event.lineSheetId(), null, null,
                event.properties())));
    }
}
