package pl.wizytowka.shared.domain;

/** Konflikt unikalności (zajęty slug, SKU, EAN) lub zasób w użyciu — HTTP 409. */
public final class ResourceConflictException extends DomainException {

    public ResourceConflictException(String code, String message) {
        super(code, message);
    }
}
