package pl.wizytowka.identity.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Zdarzenie: konto kupca zostało usunięte (przez kupca albo markę). Publikowane w transakcji usunięcia — moduły,
 * które przechowują dane kupca (zestawienia), usuwają z nich dane osobowe w tej samej transakcji.
 */
public record BuyerAccountDeleted(UUID buyerAccountId) {

    public BuyerAccountDeleted {
        Objects.requireNonNull(buyerAccountId, "buyerAccountId");
    }
}
