package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.BuyerStatus;

/**
 * Poprawne dane logowania, ale konto kupca nie jest aktywne (HTTP 403). Zgłaszany dopiero po sprawdzeniu hasła,
 * więc nie ujawnia istnienia konta osobie, która nie zna hasła. Frontend pokazuje komunikat zależny od stanu
 * (np. „potwierdź adres e-mail”, „wniosek czeka na akceptację”).
 */
public final class AccountNotActiveException extends RuntimeException {

    public static final String CODE = "identity.buyer.not-active";

    private final BuyerStatus status;

    public AccountNotActiveException(BuyerStatus status) {
        super("Buyer account is not active");
        this.status = status;
    }

    public BuyerStatus status() {
        return status;
    }
}
