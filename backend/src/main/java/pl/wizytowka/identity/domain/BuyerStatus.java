package pl.wizytowka.identity.domain;

/**
 * Cykl życia konta kupca (D23):
 * {@code PENDING_EMAIL → PENDING_APPROVAL → ACTIVE | REJECTED}, {@code ACTIVE ⇄ SUSPENDED};
 * odrzucone konto marka może jeszcze zaakceptować. Ceny widzi wyłącznie {@link #ACTIVE}.
 */
public enum BuyerStatus {
    /** Czeka na potwierdzenie adresu e-mail. */
    PENDING_EMAIL,
    /** Adres potwierdzony, czeka na decyzję marki. */
    PENDING_APPROVAL,
    ACTIVE,
    REJECTED,
    SUSPENDED
}
