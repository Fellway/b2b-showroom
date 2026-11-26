package pl.wizytowka.legal.application.view;

import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalDocumentVersion;

import java.time.Instant;
import java.util.UUID;

/** Wersja dokumentu na listach (bez treści). Dla szkicu {@code versionNumber} i {@code publishedAt} są puste. */
public record LegalVersionSummary(UUID id,
                                  LegalDocumentType type,
                                  LegalDocumentVersion.Status status,
                                  Integer versionNumber,
                                  boolean acceptanceRequired,
                                  Instant publishedAt,
                                  Instant createdAt) {

    public static LegalVersionSummary of(LegalDocumentVersion version) {
        return new LegalVersionSummary(version.id().value(), version.type(), version.status(), version.versionNumber(),
                version.acceptanceRequired(), version.publishedAt(), version.createdAt());
    }
}
