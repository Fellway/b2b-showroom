package pl.wizytowka.linesheet.application.port.out;

import pl.wizytowka.linesheet.application.view.LineSheetSummaryView;
import pl.wizytowka.linesheet.application.view.ProductLineSheetCount;
import pl.wizytowka.linesheet.domain.EmailDeliveryStatus;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;

import java.time.Instant;
import java.util.List;

public interface LineSheetReadModel {

    /** Najnowsze najpierw; {@code emailStatus == null} = bez filtra. */
    PageResult<LineSheetSummaryView> findSummaries(EmailDeliveryStatus emailStatus, PageQuery page);

    long countCreated(Instant from, Instant to);

    List<ProductLineSheetCount> topProducts(Instant from, Instant to, int limit);
}
