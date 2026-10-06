package pl.wizytowka.analytics.application.port.in;

import java.util.List;

public interface RecordAnalyticsEventsUseCase {

    int MAX_BATCH_SIZE = 50;

    /**
     * Zapisuje paczkę zdarzeń z przeglądarki (1..{@value #MAX_BATCH_SIZE}); dozwolone są wyłącznie typy
     * {@link pl.wizytowka.analytics.domain.AnalyticsEventType#isClientReportable() zgłaszalne przez klienta}.
     * Paczka jest zapisywana atomowo — jedno niepoprawne zdarzenie odrzuca całość.
     */
    void recordClientEvents(List<ClientEvent> events);

    /** Zapis w osobnej transakcji: błąd analityki nigdy nie wycofuje operacji biznesowej. */
    void recordServerEvent(ServerEvent event);
}
