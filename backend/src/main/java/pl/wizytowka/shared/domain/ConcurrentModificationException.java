package pl.wizytowka.shared.domain;

/** Zasób został zmieniony przez kogoś innego (optymistyczne blokowanie) — HTTP 409. */
public final class ConcurrentModificationException extends DomainException {

    public ConcurrentModificationException(String code, String message) {
        super(code, message);
    }
}
