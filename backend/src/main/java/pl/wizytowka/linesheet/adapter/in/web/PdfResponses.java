package pl.wizytowka.linesheet.adapter.in.web;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import pl.wizytowka.linesheet.application.view.LineSheetPdf;

/** PDF zestawienia zawiera ceny i dane klienta — nie może trafić do cache ani wyszukiwarek. */
final class PdfResponses {

    private PdfResponses() {
    }

    static ResponseEntity<InputStreamResource> attachment(LineSheetPdf pdf) {
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
