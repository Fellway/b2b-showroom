package pl.wizytowka.analytics.application.port.out;

import pl.wizytowka.analytics.domain.AnalyticsEvent;

import java.util.List;

public interface AnalyticsEventStore {

    void append(List<AnalyticsEvent> events);
}
