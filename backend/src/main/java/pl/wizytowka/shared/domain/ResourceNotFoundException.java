package pl.wizytowka.shared.domain;

/** Brak zasobu — HTTP 404. */
public final class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String code, String message) {
        super(code, message);
    }
}
