package pl.wizytowka.legal.adapter.in.web;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.legal.application.port.in.LegalDocumentQuery;
import pl.wizytowka.legal.application.view.LegalDocumentView;
import pl.wizytowka.legal.application.view.LegalVersionSummary;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.shared.adapter.in.web.RequestedLanguage;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Publiczne dokumenty prawne (D25): obowiązująca wersja regulaminu / polityki prywatności, historia wersji
 * i konkretna wersja (do historii zgód kupca). Szkice nie są dostępne.
 */
@RestController
@RequestMapping("/api/v1/legal")
class PublicLegalController {

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic();

    private final LegalDocumentQuery query;

    PublicLegalController(LegalDocumentQuery query) {
        this.query = query;
    }

    /** @param slug {@code terms} albo {@code privacy} */
    @GetMapping("/{slug}")
    ResponseEntity<LegalDocumentView> current(@PathVariable String slug, @RequestedLanguage CatalogLanguage language) {
        LegalDocumentView document = query.current(type(slug), language).orElseThrow(PublicLegalController::notFound);
        return ResponseEntity.ok().cacheControl(CACHE).body(document);
    }

    @GetMapping("/{slug}/versions")
    ResponseEntity<List<LegalVersionSummary>> versions(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CACHE).body(query.publishedVersions(type(slug)));
    }

    @GetMapping("/versions/{id}")
    ResponseEntity<LegalDocumentView> version(@PathVariable UUID id, @RequestedLanguage CatalogLanguage language) {
        LegalDocumentView document = query.publishedVersion(id, language).orElseThrow(PublicLegalController::notFound);
        // opublikowana wersja nigdy się nie zmienia
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic()).body(document);
    }

    static LegalDocumentType type(String slug) {
        return LegalDocumentType.fromSlug(slug).orElseThrow(PublicLegalController::notFound);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("legal.document.not-found", "Document not found");
    }
}
