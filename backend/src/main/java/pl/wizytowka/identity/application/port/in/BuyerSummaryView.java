package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerStatus;

import java.time.Instant;
import java.util.UUID;

/** Wiersz listy kupców w panelu. */
public record BuyerSummaryView(UUID id,
                               String email,
                               String companyName,
                               String countryCode,
                               String taxId,
                               String contactName,
                               BuyerStatus status,
                               Instant createdAt) {

    public static BuyerSummaryView of(BuyerAccount account) {
        return new BuyerSummaryView(account.id().value(), account.email(), account.companyName(),
                account.taxId().countryCode(), account.taxId().number(), account.contactName(), account.status(),
                account.createdAt());
    }
}
