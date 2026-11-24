package pl.wizytowka.identity.adapter.in.web;

import pl.wizytowka.identity.application.port.in.BuyerAccountView;
import pl.wizytowka.identity.domain.BuyerStatus;

import java.time.Instant;
import java.util.UUID;

/** Konto kupca w odpowiedziach API kupca. */
record BuyerResponse(UUID id,
                     String email,
                     String companyName,
                     String countryCode,
                     String taxId,
                     String contactName,
                     String phone,
                     String language,
                     BuyerStatus status,
                     Instant createdAt,
                     long version) {

    static BuyerResponse of(BuyerAccountView view) {
        return new BuyerResponse(view.id(), view.email(), view.companyName(), view.countryCode(), view.taxId(),
                view.contactName(), view.phone(), view.language().code(), view.status(), view.createdAt(),
                view.version());
    }
}
