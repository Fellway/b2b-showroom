package pl.wizytowka.analytics.adapter.out.persistence;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import pl.wizytowka.analytics.application.port.out.AnalyticsEventStore;
import pl.wizytowka.analytics.application.port.out.AnalyticsReadModel;
import pl.wizytowka.analytics.domain.AnalyticsEvent;
import pl.wizytowka.analytics.domain.AnalyticsEventType;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
class AnalyticsPersistenceAdapter implements AnalyticsEventStore, AnalyticsReadModel {

    private final AnalyticsEventJpaRepository repository;

    AnalyticsPersistenceAdapter(AnalyticsEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(List<AnalyticsEvent> events) {
        repository.saveAll(events.stream().map(AnalyticsEventJpaEntity::of).toList());
    }

    @Override
    public Map<AnalyticsEventType, Long> countByType(Instant from, Instant to) {
        Map<AnalyticsEventType, Long> counts = new EnumMap<>(AnalyticsEventType.class);
        repository.countByType(from, to).forEach(row -> counts.put(row.getType(), row.getTotal()));
        return counts;
    }

    @Override
    public long countUniqueVisitors(Instant from, Instant to) {
        return repository.countUniqueVisitors(from, to);
    }

    @Override
    public List<ProductCount> topProducts(AnalyticsEventType type, Instant from, Instant to, int limit) {
        return repository.topProducts(type, from, to, Limit.of(limit));
    }
}
