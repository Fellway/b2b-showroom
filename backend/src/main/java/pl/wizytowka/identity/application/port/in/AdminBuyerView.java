package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerStatus;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Konto kupca w panelu marki: dane do weryfikacji wniosku, historia decyzji i rejestr zgód (D25). */
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
                             Instant emailVerifiedAt,
                             Instant reviewedAt,
                             UUID reviewedBy,
                             String reviewNote,
                             Instant lastLoginAt,
                             Instant lockedUntil,
                             List<BuyerConsentView> consents,
                             long version) {

    public static AdminBuyerView of(BuyerAccount account, List<BuyerConsentView> consents) {
        return new AdminBuyerView(account.id().value(), account.email(), account.companyName(),
                account.taxId().countryCode(), account.taxId().number(), account.contactName(), account.phone(),
                account.language(), account.status(), account.createdAt(),
                account.emailVerifiedAt(), account.reviewedAt(),
                account.reviewedBy() == null ? null : account.reviewedBy().value(), account.reviewNote(),
                account.lastLoginAt(), account.lockedUntil(), List.copyOf(consents), account.version());
    }
}
