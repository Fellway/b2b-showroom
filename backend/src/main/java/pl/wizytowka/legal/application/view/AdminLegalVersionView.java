package pl.wizytowka.legal.application.view;

import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalDocumentVersion;
import pl.wizytowka.legal.domain.LegalText;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Wersja dokumentu w panelu — wszystkie języki naraz. */
public record AdminLegalVersionView(UUID id,
                                    LegalDocumentType type,
                                    LegalDocumentVersion.Status status,
                                    Integer versionNumber,
                                    boolean acceptanceRequired,
                                    Instant publishedAt,
                                    Instant createdAt,
                                    Map<CatalogLanguage, LegalText> texts,
                                    List<CatalogLanguage> missingLanguages,
                                    long version) {

    public static AdminLegalVersionView of(LegalDocumentVersion v) {
        return new AdminLegalVersionView(v.id().value(), v.type(), v.status(), v.versionNumber(),
                v.acceptanceRequired(), v.publishedAt(), v.createdAt(), v.texts(), v.missingLanguages(), v.version());
    }
}
