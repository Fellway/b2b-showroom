package pl.wizytowka.analytics.application.view;

import pl.wizytowka.analytics.domain.AnalyticsEventType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Podsumowanie statystyk dla zakresu [from, to).
 * @param eventTotals         liczba zdarzeń każdego typu (zawsze wszystkie typy, także z zerem)
 * @param uniqueVisitors      unikalni odwiedzający (PAGE_VIEW z identyfikatorem przeglądarki)
 * @param lineSheetsCreated   wygenerowane zestawienia PDF
 * @param topLineSheetProducts produkty najczęściej umieszczane w zestawieniach (liczba zestawień)
 */
public record AnalyticsSummaryView(Instant from,
                                   Instant to,
                                   Map<AnalyticsEventType, Long> eventTotals,
                                   long uniqueVisitors,
                                   long lineSheetsCreated,
                                   List<ProductRankingEntry> topViewedProducts,
                                   List<ProductRankingEntry> topSelectedProducts,
                                   List<ProductRankingEntry> topLineSheetProducts) {
}
