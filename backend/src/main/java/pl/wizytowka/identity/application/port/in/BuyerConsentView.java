package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.BuyerConsent;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.UUID;

/** Wpis historii zgód — dla kupca („Moje konto”), eksportu danych i panelu marki. */
public record BuyerConsentView(LegalDocumentType documentType,
                               UUID documentVersionId,
                               int versionNumber,
                               CatalogLanguage language,
                               BuyerConsent.Context context,
                               Instant acceptedAt) {

    public static BuyerConsentView of(BuyerConsent consent) {
        return new BuyerConsentView(consent.documentType(), consent.documentVersionId(), consent.versionNumber(),
                consent.language(), consent.context(), consent.acceptedAt());
    }
}
