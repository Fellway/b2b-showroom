package pl.wizytowka.analytics.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.wizytowka.analytics.application.port.out.AnalyticsEventStore;
import pl.wizytowka.analytics.application.port.out.AnalyticsReadModel;
import pl.wizytowka.analytics.application.port.out.AnalyticsReadModel.ProductCount;
import pl.wizytowka.analytics.domain.AnalyticsEvent;
import pl.wizytowka.analytics.domain.AnalyticsEventType;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.support.PersistenceTest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@PersistenceTest
class AnalyticsPersistenceAdapterTest {

    private static final Instant T0 = Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    AnalyticsEventStore store;

    @Autowired
    AnalyticsReadModel readModel;

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void aggregatesEventsInRange() {
        UUID visitor = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        store.append(List.of(
                event(AnalyticsEventType.PRODUCT_VIEW, T0, visitor, productA, Map.of()),
                event(AnalyticsEventType.PRODUCT_VIEW, T0.plusSeconds(10), visitor, productA, Map.of()),
                event(AnalyticsEventType.PRODUCT_VIEW, T0.plusSeconds(20), null, productB, Map.of()),
                event(AnalyticsEventType.PAGE_VIEW, T0, visitor, null, Map.of("utm_source", "newsletter")),
                event(AnalyticsEventType.PAGE_VIEW, T0.plusSeconds(5), visitor, null, Map.of()),
                event(AnalyticsEventType.PAGE_VIEW, T0.plusSeconds(6), UUID.randomUUID(), null, Map.of()),
                event(AnalyticsEventType.PAGE_VIEW, T0.plusSeconds(7200), UUID.randomUUID(), null, Map.of())));
        em.flush();

        Instant to = T0.plusSeconds(3600);
        assertThat(readModel.countByType(T0, to)).containsExactlyInAnyOrderEntriesOf(Map.of(
                AnalyticsEventType.PRODUCT_VIEW, 3L, AnalyticsEventType.PAGE_VIEW, 3L));
        assertThat(readModel.countUniqueVisitors(T0, to)).isEqualTo(2);
        assertThat(readModel.topProducts(AnalyticsEventType.PRODUCT_VIEW, T0, to, 1))
                .containsExactly(new ProductCount(productA, 2));
        assertThat(jdbc.queryForObject(
                "select properties ->> 'utm_source' from analytics_event where properties ? 'utm_source'", String.class))
                .isEqualTo("newsletter");
    }

    private static AnalyticsEvent event(AnalyticsEventType type, Instant at, UUID visitor, UUID product,
                                        Map<String, String> properties) {
        return new AnalyticsEvent(type, at, visitor, null, CatalogLanguage.PL, product, null, null, "/pl/katalog",
                "google.com", properties);
    }
}
