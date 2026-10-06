package pl.wizytowka.linesheet.application.port.in;

import pl.wizytowka.linesheet.application.view.ProductLineSheetCount;

import java.time.Instant;
import java.util.List;

/** Statystyki zestawień dla innych modułów (analityka), w zakresie [from, to). */
public interface LineSheetStatisticsQuery {

    long countCreated(Instant from, Instant to);

    List<ProductLineSheetCount> topProducts(Instant from, Instant to, int limit);
}
