package pl.wizytowka.shared.adapter.out.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pl.wizytowka.shared.domain.CatalogLanguage;

/** Zapisuje język jako kod ISO 639-1 ('pl', 'en', 'cs', 'uk'). Dla kluczy map użyj {@code @Convert(attributeName = "key")}. */
@Converter(autoApply = true)
public class CatalogLanguageConverter implements AttributeConverter<CatalogLanguage, String> {

    @Override
    public String convertToDatabaseColumn(CatalogLanguage language) {
        return language == null ? null : language.code();
    }

    @Override
    public CatalogLanguage convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
        return CatalogLanguage.fromCode(code)
                .orElseThrow(() -> new IllegalStateException("Unsupported language code in database: " + code));
    }
}
