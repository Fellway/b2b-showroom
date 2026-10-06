package pl.wizytowka.analytics.application.port.out;

import pl.wizytowka.analytics.domain.AnalyticsEventType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Agregaty w zakresie [from, to). */
public interface AnalyticsReadModel {

    /** Zawiera tylko typy, które wystąpiły. */
    Map<AnalyticsEventType, Long> countByType(Instant from, Instant to);

    long countUniqueVisitors(Instant from, Instant to);

    /** Ranking malejąco po liczbie zdarzeń, przy remisie po id produktu. */
    List<ProductCount> topProducts(AnalyticsEventType type, Instant from, Instant to, int limit);

    record ProductCount(UUID productId, long count) {
    }
}
