package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.Guard;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.Pricing;
import pl.wizytowka.shared.domain.Translations;

import java.util.Locale;
import java.util.Objects;

/** Edytowalne w panelu dane podstawowe produktu (bez wariantów i zdjęć). */
public record ProductDetails(CategoryId categoryId,
                             String slug,
                             String styleCode,
                             Pricing pricing,
                             boolean featured,
                             int sortOrder,
                             Translations<ProductTranslation> translations) {

    public ProductDetails {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(pricing, "pricing");
        Objects.requireNonNull(translations, "translations");
        slug = Guard.slug(slug, 160, "catalog.product.slug");
        styleCode = Guard.requiredText(styleCode, 40, "catalog.product.style-code").toUpperCase(Locale.ROOT);
        if (sortOrder < 0) {
            throw new InvalidValueException("catalog.sort-order", "Sort order must not be negative");
        }
    }
}
