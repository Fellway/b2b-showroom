package pl.wizytowka.shared.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Niezmienny zbiór tłumaczeń jednej treści. Zawsze zawiera język bazowy,
 * a odczyt w brakującym języku zwraca wersję bazową.
 */
public final class Translations<T> {

    private final Map<CatalogLanguage, T> values;

    private Translations(Map<CatalogLanguage, T> values) {
        if (!values.containsKey(CatalogLanguage.DEFAULT)) {
            throw new InvalidValueException("i18n.default-language-required",
                    "Translation in " + CatalogLanguage.DEFAULT.code() + " is required");
        }
        this.values = Collections.unmodifiableMap(values);
    }

    public static <T> Translations<T> of(Map<CatalogLanguage, T> values) {
        Objects.requireNonNull(values, "values");
        EnumMap<CatalogLanguage, T> copy = new EnumMap<>(CatalogLanguage.class);
        values.forEach((language, value) -> copy.put(Objects.requireNonNull(language), Objects.requireNonNull(value)));
        return new Translations<>(copy);
    }

    public static <T> Translations<T> ofDefault(T value) {
        return of(Map.of(CatalogLanguage.DEFAULT, value));
    }

    public T resolve(CatalogLanguage language) {
        return Optional.ofNullable(values.get(language)).orElseGet(() -> values.get(CatalogLanguage.DEFAULT));
    }

    public Optional<T> exact(CatalogLanguage language) {
        return Optional.ofNullable(values.get(language));
    }

    public Translations<T> with(CatalogLanguage language, T value) {
        EnumMap<CatalogLanguage, T> copy = new EnumMap<>(values);
        copy.put(Objects.requireNonNull(language), Objects.requireNonNull(value));
        return new Translations<>(copy);
    }

    public Map<CatalogLanguage, T> asMap() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Translations<?> other && values.equals(other.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
