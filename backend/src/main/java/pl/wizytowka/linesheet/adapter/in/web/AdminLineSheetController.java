package pl.wizytowka.linesheet.adapter.in.web;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.linesheet.application.port.in.AdminLineSheetQuery;
import pl.wizytowka.linesheet.application.port.in.GetLineSheetPdfUseCase;
import pl.wizytowka.linesheet.application.view.LineSheetDetailView;
import pl.wizytowka.linesheet.application.view.LineSheetSummaryView;
import pl.wizytowka.linesheet.domain.LineSheetId;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/line-sheets")
class AdminLineSheetController {

    private final AdminLineSheetQuery query;
    private final GetLineSheetPdfUseCase getPdf;

    AdminLineSheetController(AdminLineSheetQuery query, GetLineSheetPdfUseCase getPdf) {
        this.query = query;
        this.getPdf = getPdf;
    }

    @GetMapping
    PageResponse<LineSheetSummaryView> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        PageResult<LineSheetSummaryView> result = query.lineSheets(new PageQuery(page, size));
        return PageResponse.of(result);
    }

    @GetMapping("/{id}")
    LineSheetDetailView get(@PathVariable UUID id) {
        return query.lineSheet(LineSheetId.of(id));
    }

    /** PDF dla zespołu marki (żądanie z tokenem panelu, np. fetch → blob). */
    @GetMapping("/{id}/pdf")
    ResponseEntity<InputStreamResource> pdf(@PathVariable UUID id) {
        return PdfResponses.attachment(getPdf.pdfForStaff(LineSheetId.of(id)));
    }
}
