package pl.wizytowka.linesheet.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.linesheet.application.port.in.BuyerLineSheetQuery;
import pl.wizytowka.linesheet.application.port.in.CreateLineSheetUseCase;
import pl.wizytowka.linesheet.application.port.in.GetLineSheetPdfUseCase;
import pl.wizytowka.linesheet.application.view.CreatedLineSheet;
import pl.wizytowka.linesheet.application.view.LineSheetSummaryView;
import pl.wizytowka.shared.adapter.in.web.RequestedLanguage;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.net.URI;
import java.util.UUID;

/**
 * Zestawienia zalogowanego kupca (rola BUYER — reguła w SecurityConfig, D23): generowanie, lista „Moje zestawienia”
 * i pobranie PDF. PDF pobiera wyłącznie właściciel (żądanie z tokenem, np. fetch → blob w przeglądarce).
 */
@RestController
@RequestMapping(BuyerLineSheetController.BASE_PATH)
class BuyerLineSheetController {

    static final String BASE_PATH = "/api/v1/buyer/line-sheets";

    private final CreateLineSheetUseCase createLineSheet;
    private final BuyerLineSheetQuery query;
    private final GetLineSheetPdfUseCase getPdf;

    BuyerLineSheetController(CreateLineSheetUseCase createLineSheet, BuyerLineSheetQuery query,
                             GetLineSheetPdfUseCase getPdf) {
        this.createLineSheet = createLineSheet;
        this.query = query;
        this.getPdf = getPdf;
    }

    @PostMapping
    ResponseEntity<CreatedLineSheetResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody CreateLineSheetRequest request,
                                                    @RequestedLanguage CatalogLanguage language) {
        CreatedLineSheet created = createLineSheet.create(request.toCommand(buyer(jwt), language));
        String pdfUrl = BASE_PATH + "/" + created.publicId() + "/pdf";
        return ResponseEntity.created(URI.create(pdfUrl))
                .cacheControl(CacheControl.noStore())
                .body(new CreatedLineSheetResponse(created.publicId(), created.referenceNumber(), pdfUrl));
    }

    @GetMapping
    ResponseEntity<PageResponse<LineSheetSummaryView>> list(@AuthenticationPrincipal Jwt jwt,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(PageResponse.of(query.lineSheetsOfBuyer(buyer(jwt), new PageQuery(page, size))));
    }

    @GetMapping("/{publicId}/pdf")
    ResponseEntity<InputStreamResource> pdf(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID publicId) {
        return PdfResponses.attachment(getPdf.pdfForBuyer(buyer(jwt), publicId));
    }

    private static UUID buyer(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
