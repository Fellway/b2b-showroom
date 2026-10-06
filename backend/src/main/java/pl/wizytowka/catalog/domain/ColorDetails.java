package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.Guard;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.Translations;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @param code         wewnętrzny kod koloru, np. NAVY-02
 * @param hexValue     próbka koloru do UI w formacie #RRGGBB (opcjonalna)
 * @param translations nazwa koloru w językach katalogu
 */
public record ColorDetails(String code, String hexValue, int sortOrder, Translations<String> translations) {

    private static final Pattern HEX = Pattern.compile("^#[0-9A-F]{6}$");

    public ColorDetails {
        code = Guard.requiredText(code, 40, "catalog.color.code").toUpperCase(Locale.ROOT);
        hexValue = Guard.optionalText(hexValue, 7, "catalog.color.hex");
        if (hexValue != null) {
            hexValue = hexValue.toUpperCase(Locale.ROOT);
            if (!HEX.matcher(hexValue).matches()) {
                throw new InvalidValueException("catalog.color.hex", "Hex value must look like #RRGGBB");
            }
        }
        if (sortOrder < 0) {
            throw new InvalidValueException("catalog.sort-order", "Sort order must not be negative");
        }
        Objects.requireNonNull(translations, "translations");
        translations.asMap().values().forEach(name -> Guard.requiredText(name, 80, "catalog.color.name"));
    }
}
