package pl.wizytowka.linesheet.domain;

public enum EmailDeliveryStatus {
    /** Klient tylko pobrał PDF. */
    NOT_REQUESTED,
    /** Zlecono wysyłkę, czeka na (ponowną) próbę. */
    PENDING,
    SENT,
    /** Ostatnia próba nieudana; można zlecić ponowną wysyłkę. */
    FAILED
}
