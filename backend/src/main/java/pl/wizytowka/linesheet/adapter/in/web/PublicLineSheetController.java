package pl.wizytowka.linesheet.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.linesheet.application.port.in.CreateLineSheetUseCase;
import pl.wizytowka.linesheet.application.port.in.GetLineSheetPdfUseCase;
import pl.wizytowka.linesheet.application.view.CreatedLineSheet;
import pl.wizytowka.linesheet.application.view.LineSheetPdf;
import pl.wizytowka.shared.adapter.in.web.RequestedLanguage;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/line-sheets")
class PublicLineSheetController {

    private final CreateLineSheetUseCase createLineSheet;
    private final GetLineSheetPdfUseCase getPdf;

    PublicLineSheetController(CreateLineSheetUseCase createLineSheet, GetLineSheetPdfUseCase getPdf) {
        this.createLineSheet = createLineSheet;
        this.getPdf = getPdf;
    }

    @PostMapping
    ResponseEntity<CreatedLineSheetResponse> create(@Valid @RequestBody CreateLineSheetRequest request,
                                                    @RequestedLanguage CatalogLanguage language) {
        CreatedLineSheet created = createLineSheet.create(request.toCommand(language));
        String pdfUrl = "/api/v1/line-sheets/" + created.publicId() + "/pdf";
        return ResponseEntity.created(URI.create(pdfUrl))
                .cacheControl(CacheControl.noStore())
                .body(new CreatedLineSheetResponse(created.publicId(), created.referenceNumber(), pdfUrl));
    }

    /** Link jest nieodgadywalny (UUIDv4), ale zawiera dane klienta — nie może trafić do cache ani wyszukiwarek. */
    @GetMapping("/{publicId}/pdf")
    ResponseEntity<InputStreamResource> pdf(@PathVariable UUID publicId) {
        LineSheetPdf pdf = getPdf.pdf(publicId);
        String fileName = pdf.fileName().replaceAll("[^A-Za-z0-9._-]", "_");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header("X-Robots-Tag", "noindex, nofollow")
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(pdf.content()));
    }
}
