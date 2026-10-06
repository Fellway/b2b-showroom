package pl.wizytowka.analytics.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.analytics.application.port.in.AnalyticsDashboardUseCase;
import pl.wizytowka.analytics.application.view.AnalyticsSummaryView;
import pl.wizytowka.shared.adapter.in.web.RequestedLanguage;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/admin/analytics")
class AnalyticsDashboardController {

    private final AnalyticsDashboardUseCase dashboard;

    AnalyticsDashboardController(AnalyticsDashboardUseCase dashboard) {
        this.dashboard = dashboard;
    }

    /**
     * @param from ISO instant (2026-10-01T00:00:00Z) lub data (2026-10-01 = początek dnia UTC)
     * @param to   ISO instant lub data (2026-10-31 = do końca tego dnia UTC, włącznie)
     */
    @GetMapping("/summary")
    AnalyticsSummaryView summary(@RequestParam(required = false) String from,
                                 @RequestParam(required = false) String to,
                                 @RequestedLanguage CatalogLanguage language) {
        return dashboard.summary(parse(from, false), parse(to, true), language);
    }

    private static Instant parse(String value, boolean endOfRange) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            if (value.length() == 10) {
                LocalDate date = LocalDate.parse(value);
                return (endOfRange ? date.plusDays(1) : date).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidValueException("analytics.range.format", "Use ISO date (YYYY-MM-DD) or instant");
        }
    }
}
