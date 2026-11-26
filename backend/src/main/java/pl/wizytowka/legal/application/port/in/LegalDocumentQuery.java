package pl.wizytowka.legal.application.port.in;

import pl.wizytowka.legal.application.view.LegalDocumentView;
import pl.wizytowka.legal.application.view.LegalRequirement;
import pl.wizytowka.legal.application.view.LegalVersionRef;
import pl.wizytowka.legal.application.view.LegalVersionSummary;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Odczyt opublikowanych dokumentów prawnych — strony publiczne i rejestr zgód kupców. Szkice nie są tu widoczne. */
public interface LegalDocumentQuery {

    /** Obowiązująca wersja dokumentu. */
    Optional<LegalDocumentView> current(LegalDocumentType type, CatalogLanguage language);

    /** Konkretna opublikowana wersja (np. ta, którą kupiec zaakceptował w przeszłości). */
    Optional<LegalDocumentView> publishedVersion(UUID versionId, CatalogLanguage language);

    /** Historia opublikowanych wersji, od najnowszej. */
    List<LegalVersionSummary> publishedVersions(LegalDocumentType type);

    /** Wymagania wobec kupców — po jednym dla każdego rodzaju, który ma opublikowaną wersję. */
    List<LegalRequirement> requirements();

    Optional<LegalRequirement> requirement(LegalDocumentType type);

    Optional<LegalVersionRef> publishedVersionRef(UUID versionId);
}
