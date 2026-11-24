package pl.wizytowka.linesheet.application.port.in;

import pl.wizytowka.linesheet.application.view.LineSheetSummaryView;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;

import java.util.UUID;

/** „Moje zestawienia” zalogowanego kupca (D23). */
public interface BuyerLineSheetQuery {

    /** Najnowsze najpierw. */
    PageResult<LineSheetSummaryView> lineSheetsOfBuyer(UUID buyerAccountId, PageQuery page);
}
