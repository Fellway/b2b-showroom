package pl.wizytowka.shared.application;

import pl.wizytowka.shared.domain.InvalidValueException;

/** Żądanie strony wyników, niezależne od Spring Data. Numeracja od 0. */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new InvalidValueException("page.invalid", "Page index must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidValueException("page.size-invalid", "Page size must be between 1 and " + MAX_SIZE);
        }
    }

    public long offset() {
        return (long) page * size;
    }
}
