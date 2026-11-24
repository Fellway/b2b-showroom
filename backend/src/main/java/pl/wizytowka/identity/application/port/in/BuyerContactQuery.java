package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.Optional;
import java.util.UUID;

/** Dane kontaktowe kupca dla innych modułów (zestawienia PDF). */
public interface BuyerContactQuery {

    /** Dane aktywnego kupca; pusty wynik, gdy konto nie istnieje lub nie jest aktywne. */
    Optional<BuyerContact> activeBuyer(UUID buyerAccountId);

    /** @param taxId numer z krajem do wyświetlenia, np. {@code PL 1234563218} */
    record BuyerContact(UUID id, String companyName, String contactName, String email, String phone, String taxId,
                        CatalogLanguage language) {
    }
}
