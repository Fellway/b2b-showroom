package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerStatus;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.UUID;

/** Konto kupca w panelu marki: dane do weryfikacji wniosku i historia decyzji. */
public record AdminBuyerView(UUID id,
                             String email,
                             String companyName,
                             String countryCode,
                             String taxId,
                             String contactName,
                             String phone,
                             CatalogLanguage language,
                             BuyerStatus status,
                             Instant createdAt,
                             Instant termsAcceptedAt,
                             Instant privacyAcceptedAt,
                             Instant emailVerifiedAt,
                             Instant reviewedAt,
                             UUID reviewedBy,
                             String reviewNote,
                             Instant lastLoginAt,
                             Instant lockedUntil,
                             long version) {

    public static AdminBuyerView of(BuyerAccount account) {
        return new AdminBuyerView(account.id().value(), account.email(), account.companyName(),
                account.taxId().countryCode(), account.taxId().number(), account.contactName(), account.phone(),
                account.language(), account.status(), account.createdAt(), account.termsAcceptedAt(),
                account.privacyAcceptedAt(), account.emailVerifiedAt(), account.reviewedAt(),
                account.reviewedBy() == null ? null : account.reviewedBy().value(), account.reviewNote(),
                account.lastLoginAt(), account.lockedUntil(), account.version());
    }
}
