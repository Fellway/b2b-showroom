package pl.wizytowka.shared.domain;

/** Operacja niedozwolona w bieżącym stanie (np. publikacja bez wariantów) — HTTP 422. */
public final class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String code, String message) {
        super(code, message);
    }
}
