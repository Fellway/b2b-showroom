package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.Guard;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.util.Locale;
import java.util.Objects;

/** Słownik rozmiarów; {@code sortOrder} wyznacza kolejność na rozmiarówce (XS, S, M... lub 36, 38...). */
public final class Size {

    private final SizeId id;
    private String code;
    private int sortOrder;
    private final long version;

    private Size(SizeId id, String code, int sortOrder, long version) {
        this.id = Objects.requireNonNull(id);
        this.version = version;
        update(code, sortOrder);
    }

    public static Size create(String code, int sortOrder) {
        return new Size(SizeId.newId(), code, sortOrder, 0);
    }

    public static Size restore(SizeId id, String code, int sortOrder, long version) {
        return new Size(id, code, sortOrder, version);
    }

    public void update(String code, int sortOrder) {
        this.code = Guard.requiredText(code, 16, "catalog.size.code").toUpperCase(Locale.ROOT);
        if (sortOrder < 0) {
            throw new InvalidValueException("catalog.sort-order", "Sort order must not be negative");
        }
        this.sortOrder = sortOrder;
    }

    public SizeId id() {
        return id;
    }

    public String code() {
        return code;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public long version() {
        return version;
    }
}
