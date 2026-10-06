package pl.wizytowka.analytics.application.service;

import org.junit.jupiter.api.Test;
import pl.wizytowka.analytics.application.port.in.ClientEvent;
import pl.wizytowka.analytics.application.port.out.AnalyticsReadModel;
import pl.wizytowka.analytics.application.view.AnalyticsSummaryView;
import pl.wizytowka.analytics.application.view.ProductRankingEntry;
import pl.wizytowka.analytics.domain.AnalyticsEvent;
import pl.wizytowka.analytics.domain.AnalyticsEventType;
import pl.wizytowka.catalog.application.port.in.ProductLabelQuery;
import pl.wizytowka.catalog.application.view.ProductLabel;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.ProductStatus;
import pl.wizytowka.linesheet.application.port.in.LineSheetStatisticsQuery;
import pl.wizytowka.linesheet.application.view.ProductLineSheetCount;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalyticsServicesTest {

    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void recordsClientEventsWithServerTime() {
        List<AnalyticsEvent> stored = new ArrayList<>();
        AnalyticsRecordingService service = new AnalyticsRecordingService(stored::addAll, CLOCK);

        service.recordClientEvents(List.of(client(AnalyticsEventType.PRODUCT_VIEW), client(AnalyticsEventType.PAGE_VIEW)));

        assertThat(stored).hasSize(2).allSatisfy(e -> assertThat(e.occurredAt()).isEqualTo(NOW));
    }

    @Test
    void rejectsServerOnlyTypesAndOversizedBatchesFromClient() {
        List<AnalyticsEvent> stored = new ArrayList<>();
        AnalyticsRecordingService service = new AnalyticsRecordingService(stored::addAll, CLOCK);

        assertThatThrownBy(() -> service.recordClientEvents(List.of(client(AnalyticsEventType.LINE_SHEET_GENERATED))))
                .hasFieldOrPropertyWithValue("code", "analytics.event.type-not-allowed");
        assertThatThrownBy(() -> service.recordClientEvents(
                Collections.nCopies(51, client(AnalyticsEventType.PAGE_VIEW))))
                .hasFieldOrPropertyWithValue("code", "analytics.batch.size");
        assertThatThrownBy(() -> service.recordClientEvents(List.of(client(AnalyticsEventType.PAGE_VIEW),
                new ClientEvent(AnalyticsEventType.PAGE_VIEW, null, null, null, null, null, "no-slash", null, null))))
                .isInstanceOf(InvalidValueException.class);
        assertThat(stored).as("paczka zapisywana atomowo").isEmpty();
    }

    @Test
    void summaryDefaultsToLast30DaysAndLabelsProducts() {
        UUID known = UUID.randomUUID();
        UUID deleted = UUID.randomUUID();
        AnalyticsReadModel readModel = new AnalyticsReadModel() {
            @Override
            public Map<AnalyticsEventType, Long> countByType(Instant from, Instant to) {
                return Map.of(AnalyticsEventType.PAGE_VIEW, 120L);
            }

            @Override
            public long countUniqueVisitors(Instant from, Instant to) {
                return 40;
            }

            @Override
            public List<ProductCount> topProducts(AnalyticsEventType type, Instant from, Instant to, int limit) {
                return type == AnalyticsEventType.PRODUCT_VIEW
                        ? List.of(new ProductCount(known, 9), new ProductCount(deleted, 3))
                        : List.of();
            }
        };
        LineSheetStatisticsQuery lineSheets = new LineSheetStatisticsQuery() {
            @Override
            public long countCreated(Instant from, Instant to) {
                return 5;
            }

            @Override
            public List<ProductLineSheetCount> topProducts(Instant from, Instant to, int limit) {
                return List.of(new ProductLineSheetCount(known, 4));
            }
        };
        ProductLabelQuery labels = (ids, language) -> Map.of(ProductId.of(known),
                new ProductLabel(known, "koszula", "LS-100", "Linen shirt", ProductStatus.PUBLISHED));
        AnalyticsDashboardService service = new AnalyticsDashboardService(readModel, lineSheets, labels, CLOCK);

        AnalyticsSummaryView summary = service.summary(null, null, CatalogLanguage.EN);

        assertThat(summary.to()).isEqualTo(NOW);
        assertThat(summary.from()).isEqualTo(NOW.minus(Duration.ofDays(30)));
        assertThat(summary.eventTotals()).hasSize(AnalyticsEventType.values().length)
                .containsEntry(AnalyticsEventType.PAGE_VIEW, 120L)
                .containsEntry(AnalyticsEventType.CONTACT_CLICKED, 0L);
        assertThat(summary.uniqueVisitors()).isEqualTo(40);
        assertThat(summary.lineSheetsCreated()).isEqualTo(5);
        assertThat(summary.topViewedProducts()).containsExactly(
                new ProductRankingEntry(known, true, "koszula", "LS-100", "Linen shirt", "PUBLISHED", 9),
                new ProductRankingEntry(deleted, false, null, null, null, null, 3));
        assertThat(summary.topLineSheetProducts()).extracting(ProductRankingEntry::count).containsExactly(4L);
    }

    @Test
    void rejectsInvalidAndTooLongRanges() {
        AnalyticsDashboardService service = new AnalyticsDashboardService(null, null, null, CLOCK);

        assertThatThrownBy(() -> service.summary(NOW, NOW.minusSeconds(1), CatalogLanguage.PL))
                .hasFieldOrPropertyWithValue("code", "analytics.range.invalid");
        assertThatThrownBy(() -> service.summary(NOW.minus(Duration.ofDays(367)), NOW, CatalogLanguage.PL))
                .hasFieldOrPropertyWithValue("code", "analytics.range.too-long");
    }

    private static ClientEvent client(AnalyticsEventType type) {
        return new ClientEvent(type, UUID.randomUUID(), null, CatalogLanguage.PL, null, null, "/pl", null, Map.of());
    }

}
