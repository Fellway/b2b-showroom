package pl.wizytowka.legal.application.view;

import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.UUID;

/**
 * Opublikowana wersja dokumentu w jednym języku (strony publiczne, akceptacja przez kupca).
 *
 * @param language język treści — może być bazowym, gdy brakuje tłumaczenia
 */
public record LegalDocumentView(UUID versionId,
                                LegalDocumentType type,
                                int versionNumber,
                                Instant publishedAt,
                                boolean acceptanceRequired,
                                CatalogLanguage language,
                                String title,
                                String body,
                                String changeSummary) {
}
