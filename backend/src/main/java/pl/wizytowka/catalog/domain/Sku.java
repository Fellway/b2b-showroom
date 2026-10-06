package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.InvalidValueException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Wewnętrzny kod magazynowy wariantu, normalizowany do wielkich liter. */
public record Sku(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9][A-Z0-9._/-]{0,63}$");

    public Sku {
        Objects.requireNonNull(value, "SKU value");
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidValueException("catalog.sku.format",
                    "SKU may contain letters, digits and . _ / - (max 64 characters)");
        }
    }

    public static Sku of(String value) {
        return new Sku(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
