package pl.wizytowka.catalog.adapter.in.web.dto;

import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.Translations;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Konwersja map tłumaczeń z JSON (klucz = kod języka, np. "pl") na model domenowy i z powrotem.
 * Brak wpisu "pl" odrzuca domena ({@link Translations}).
 */
public final class LanguageMaps {

    private LanguageMaps() {
    }

    public static <S, T> Translations<T> toTranslations(Map<String, S> source, Function<? super S, ? extends T> mapper) {
        EnumMap<CatalogLanguage, T> values = new EnumMap<>(CatalogLanguage.class);
        if (source != null) {
            source.forEach((code, value) -> {
                CatalogLanguage language = CatalogLanguage.fromCode(code)
                        .orElseThrow(() -> new InvalidValueException("i18n.unsupported-language",
                                "Supported languages: pl, en, cs, uk"));
                if (value == null) {
                    throw new InvalidValueException("i18n.translation-missing", "Translation for " + code + " is empty");
                }
                values.put(language, mapper.apply(value));
            });
        }
        return Translations.of(values);
    }

    /** Mapa z kluczami w postaci kodów języków, w stałej kolejności (pl, en, cs, uk). */
    public static <S, T> Map<String, T> toCodes(Map<CatalogLanguage, S> source, Function<? super S, ? extends T> mapper) {
        Map<String, T> result = new LinkedHashMap<>();
        new EnumMap<>(source).forEach((language, value) -> result.put(language.code(), mapper.apply(value)));
        return result;
    }
}
