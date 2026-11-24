package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerStatus;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.UUID;

/** Konto kupca widziane przez samego kupca (bez notatek marki i danych technicznych). */
public record BuyerAccountView(UUID id,
                               String email,
                               String companyName,
                               String countryCode,
                               String taxId,
                               String contactName,
                               String phone,
                               CatalogLanguage language,
                               BuyerStatus status,
                               Instant createdAt,
                               long version) {

    public static BuyerAccountView of(BuyerAccount account) {
        return new BuyerAccountView(account.id().value(), account.email(), account.companyName(),
                account.taxId().countryCode(), account.taxId().number(), account.contactName(), account.phone(),
                account.language(), account.status(), account.createdAt(), account.version());
    }
}
