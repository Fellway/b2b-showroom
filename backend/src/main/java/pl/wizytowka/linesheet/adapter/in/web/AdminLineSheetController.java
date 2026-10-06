package pl.wizytowka.linesheet.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.linesheet.application.port.in.AdminLineSheetQuery;
import pl.wizytowka.linesheet.application.port.in.ResendLineSheetEmailUseCase;
import pl.wizytowka.linesheet.application.view.LineSheetDetailView;
import pl.wizytowka.linesheet.application.view.LineSheetSummaryView;
import pl.wizytowka.linesheet.domain.EmailDeliveryStatus;
import pl.wizytowka.linesheet.domain.LineSheetId;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/line-sheets")
class AdminLineSheetController {

    private final AdminLineSheetQuery query;
    private final ResendLineSheetEmailUseCase resendEmail;

    AdminLineSheetController(AdminLineSheetQuery query, ResendLineSheetEmailUseCase resendEmail) {
        this.query = query;
        this.resendEmail = resendEmail;
    }

    @GetMapping
    PageResponse<LineSheetSummaryView> list(@RequestParam(required = false) EmailDeliveryStatus emailStatus,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        PageResult<LineSheetSummaryView> result = query.lineSheets(emailStatus, new PageQuery(page, size));
        return PageResponse.of(result);
    }

    @GetMapping("/{id}")
    LineSheetDetailView get(@PathVariable UUID id) {
        return query.lineSheet(LineSheetId.of(id));
    }

    @PostMapping("/{id}/resend-email")
    ResponseEntity<Void> resendEmail(@PathVariable UUID id) {
        resendEmail.resend(LineSheetId.of(id));
        return ResponseEntity.accepted().build();
    }
}
