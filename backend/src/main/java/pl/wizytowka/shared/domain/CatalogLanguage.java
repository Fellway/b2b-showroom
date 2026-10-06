package pl.wizytowka.shared.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Języki interfejsu i treści katalogu, identyfikowane kodem ISO 639-1.
 * {@link #PL} jest językiem bazowym: każda tłumaczona treść musi go mieć i służy jako fallback.
 */
public enum CatalogLanguage {
    PL("pl"),
    EN("en"),
    CS("cs"),
    UK("uk");

    public static final CatalogLanguage DEFAULT = PL;

    private final String code;

    CatalogLanguage(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public Locale toLocale() {
        return Locale.forLanguageTag(code);
    }

    public static Optional<CatalogLanguage> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalized = code.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(l -> l.code.equals(normalized)).findFirst();
    }
}
