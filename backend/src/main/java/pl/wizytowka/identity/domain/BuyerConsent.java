package pl.wizytowka.identity.domain;

import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.Uuids;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Wpis rejestru zgód (D25): kupiec zaakceptował regulamin albo przyjął do wiadomości politykę prywatności w konkretnej
 * wersji. Wpisy są tylko dopisywane — nigdy zmieniane. Bez adresu IP i User-Agenta.
 *
 * @param language język, w którym kupiec widział dokument
 */
public record BuyerConsent(UUID id,
                           BuyerAccountId buyerAccountId,
                           LegalDocumentType documentType,
                           UUID documentVersionId,
                           int versionNumber,
                           CatalogLanguage language,
                           Context context,
                           Instant acceptedAt) {

    /** Okoliczności złożenia oświadczenia. */
    public enum Context {
        /** Formularz rejestracji. */
        REGISTRATION,
        /** Akceptacja nowej wersji po zalogowaniu. */
        UPDATE
    }

    public BuyerConsent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(buyerAccountId, "buyerAccountId");
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(documentVersionId, "documentVersionId");
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(acceptedAt, "acceptedAt");
        if (versionNumber < 1) {
            throw new IllegalArgumentException("versionNumber must be positive");
        }
    }

    public static BuyerConsent record(BuyerAccountId buyer, LegalDocumentType type, UUID versionId, int versionNumber,
                                      CatalogLanguage language, Context context, Instant now) {
        return new BuyerConsent(Uuids.v7(), buyer, type, versionId, versionNumber, language, context, now);
    }
}
