package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.BuyerStatus;

/**
 * Filtr listy kupców w panelu; oba pola opcjonalne.
 * @param search fragment e-maila, nazwy firmy, osoby kontaktowej lub numeru podatkowego
 */
public record BuyerFilter(BuyerStatus status, String search) {

    public BuyerFilter {
        search = search == null || search.isBlank() ? null : search.strip();
        if (search != null && search.length() > 100) {
            search = search.substring(0, 100);
        }
    }

    public static BuyerFilter all() {
        return new BuyerFilter(null, null);
    }
}
