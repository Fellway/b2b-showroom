package pl.wizytowka.linesheet.application.port.in;

import pl.wizytowka.linesheet.application.view.LineSheetDetailView;
import pl.wizytowka.linesheet.application.view.LineSheetSummaryView;
import pl.wizytowka.linesheet.domain.EmailDeliveryStatus;
import pl.wizytowka.linesheet.domain.LineSheetId;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;

public interface AdminLineSheetQuery {

    /** Najnowsze najpierw. @param emailStatus filtr opcjonalny */
    PageResult<LineSheetSummaryView> lineSheets(EmailDeliveryStatus emailStatus, PageQuery page);

    /** @throws pl.wizytowka.shared.domain.ResourceNotFoundException */
    LineSheetDetailView lineSheet(LineSheetId id);
}
