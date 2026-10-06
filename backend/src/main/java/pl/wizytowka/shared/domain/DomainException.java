package pl.wizytowka.shared.domain;

import java.util.Objects;

/**
 * Błędy, które warstwa domeny i aplikacji zgłasza w sposób jawny. Hierarchia jest zamknięta,
 * dzięki czemu adapter HTTP mapuje je na statusy wyczerpującym switchem.
 * {@code code} to stabilny, maszynowy klucz (frontend tłumaczy go na komunikat w języku użytkownika).
 */
public abstract sealed class DomainException extends RuntimeException
        permits InvalidValueException, BusinessRuleViolationException, ResourceNotFoundException,
                ResourceConflictException, ConcurrentModificationException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public String code() {
        return code;
    }
}
