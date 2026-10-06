package pl.wizytowka.analytics.application.port.in;

import pl.wizytowka.analytics.application.view.AnalyticsSummaryView;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;

public interface AnalyticsDashboardUseCase {

    int MAX_RANGE_DAYS = 366;
    int DEFAULT_RANGE_DAYS = 30;
    int RANKING_SIZE = 10;

    /**
     * @param from początek zakresu (włącznie); {@code null} = {@code to} minus {@value #DEFAULT_RANGE_DAYS} dni
     * @param to   koniec zakresu (wyłącznie); {@code null} = teraz
     * @param language język nazw produktów w rankingach
     */
    AnalyticsSummaryView summary(Instant from, Instant to, CatalogLanguage language);
}
