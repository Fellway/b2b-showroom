package pl.wizytowka.analytics.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.analytics.application.port.in.AnalyticsDashboardUseCase;
import pl.wizytowka.analytics.application.port.out.AnalyticsReadModel;
import pl.wizytowka.analytics.application.port.out.AnalyticsReadModel.ProductCount;
import pl.wizytowka.analytics.application.view.AnalyticsSummaryView;
import pl.wizytowka.analytics.application.view.ProductRankingEntry;
import pl.wizytowka.analytics.domain.AnalyticsEventType;
import pl.wizytowka.catalog.application.port.in.ProductLabelQuery;
import pl.wizytowka.catalog.application.view.ProductLabel;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.linesheet.application.port.in.LineSheetStatisticsQuery;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
class AnalyticsDashboardService implements AnalyticsDashboardUseCase {

    private final AnalyticsReadModel readModel;
    private final LineSheetStatisticsQuery lineSheetStatistics;
    private final ProductLabelQuery productLabels;
    private final Clock clock;

    AnalyticsDashboardService(AnalyticsReadModel readModel, LineSheetStatisticsQuery lineSheetStatistics,
                              ProductLabelQuery productLabels, Clock clock) {
        this.readModel = readModel;
        this.lineSheetStatistics = lineSheetStatistics;
        this.productLabels = productLabels;
        this.clock = clock;
    }

    @Override
    public AnalyticsSummaryView summary(Instant from, Instant to, CatalogLanguage language) {
        Instant end = to != null ? to : clock.instant();
        Instant start = from != null ? from : end.minus(Duration.ofDays(DEFAULT_RANGE_DAYS));
        if (!start.isBefore(end)) {
            throw new InvalidValueException("analytics.range.invalid", "'from' must be before 'to'");
        }
        if (Duration.between(start, end).compareTo(Duration.ofDays(MAX_RANGE_DAYS)) > 0) {
            throw new InvalidValueException("analytics.range.too-long", "Range must not exceed " + MAX_RANGE_DAYS + " days");
        }

        Map<AnalyticsEventType, Long> totals = new EnumMap<>(AnalyticsEventType.class);
        for (AnalyticsEventType type : AnalyticsEventType.values()) {
            totals.put(type, 0L);
        }
        totals.putAll(readModel.countByType(start, end));

        List<ProductCount> viewed = readModel.topProducts(AnalyticsEventType.PRODUCT_VIEW, start, end, RANKING_SIZE);
        List<ProductCount> selected = readModel.topProducts(AnalyticsEventType.SELECTION_ADDED, start, end, RANKING_SIZE);
        List<ProductCount> inLineSheets = lineSheetStatistics.topProducts(start, end, RANKING_SIZE).stream()
                .map(c -> new ProductCount(c.productId(), c.lineSheetCount()))
                .toList();

        Set<ProductId> productIds = Stream.of(viewed, selected, inLineSheets)
                .flatMap(List::stream)
                .map(c -> ProductId.of(c.productId()))
                .collect(Collectors.toSet());
        Map<ProductId, ProductLabel> labels = productIds.isEmpty()
                ? Map.of()
                : productLabels.labels(productIds, language);

        return new AnalyticsSummaryView(start, end, Collections.unmodifiableMap(totals),
                readModel.countUniqueVisitors(start, end),
                lineSheetStatistics.countCreated(start, end),
                ranking(viewed, labels), ranking(selected, labels), ranking(inLineSheets, labels));
    }

    private static List<ProductRankingEntry> ranking(List<ProductCount> counts, Map<ProductId, ProductLabel> labels) {
        return counts.stream().map(count -> {
            UUID productId = count.productId();
            ProductLabel label = labels.get(ProductId.of(productId));
            return label == null
                    ? new ProductRankingEntry(productId, false, null, null, null, null, count.count())
                    : new ProductRankingEntry(productId, true, label.slug(), label.styleCode(), label.name(),
                    label.status().name(), count.count());
        }).toList();
    }
}
