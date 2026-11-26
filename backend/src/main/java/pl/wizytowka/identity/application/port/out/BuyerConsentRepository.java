package pl.wizytowka.identity.application.port.out;

import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.BuyerConsent;
import pl.wizytowka.legal.domain.LegalDocumentType;

import java.util.List;

/** Rejestr zgód kupców — tylko dopisywanie i odczyt. */
public interface BuyerConsentRepository {

    void save(BuyerConsent consent);

    /** Od najnowszego wpisu. */
    List<BuyerConsent> findByBuyer(BuyerAccountId buyer);

    /** Czy kupiec zaakceptował wersję dokumentu o numerze co najmniej {@code minimumVersionNumber}. */
    boolean hasAccepted(BuyerAccountId buyer, LegalDocumentType type, int minimumVersionNumber);
}
