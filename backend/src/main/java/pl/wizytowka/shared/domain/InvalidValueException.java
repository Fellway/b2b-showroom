package pl.wizytowka.shared.domain;

/** Niepoprawna wartość (format, zakres, długość) — HTTP 400. */
public final class InvalidValueException extends DomainException {

    public InvalidValueException(String code, String message) {
        super(code, message);
    }
}
