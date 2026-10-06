package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.InvalidValueException;

import java.util.Objects;
import java.util.regex.Pattern;

/** Kod GTIN (EAN-8, UPC-A/GTIN-12, EAN-13, GTIN-14) z walidacją cyfry kontrolnej (algorytm GS1 mod 10). */
public record Gtin(String value) {

    private static final Pattern FORMAT = Pattern.compile("\\d{8}|\\d{12,14}");

    public Gtin {
        Objects.requireNonNull(value, "GTIN value");
        value = value.strip();
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidValueException("catalog.ean.format", "EAN must have 8, 12, 13 or 14 digits");
        }
        if (!hasValidCheckDigit(value)) {
            throw new InvalidValueException("catalog.ean.check-digit", "EAN check digit is invalid");
        }
    }

    public static Gtin of(String value) {
        return new Gtin(value);
    }

    public static boolean isValid(String value) {
        return value != null && FORMAT.matcher(value.strip()).matches() && hasValidCheckDigit(value.strip());
    }

    private static boolean hasValidCheckDigit(String digits) {
        int sum = 0;
        int last = digits.length() - 1;
        for (int i = last - 1, weightPosition = 0; i >= 0; i--, weightPosition++) {
            int digit = digits.charAt(i) - '0';
            sum += weightPosition % 2 == 0 ? digit * 3 : digit;
        }
        int expected = (10 - sum % 10) % 10;
        return expected == digits.charAt(last) - '0';
    }

    @Override
    public String toString() {
        return value;
    }
}
