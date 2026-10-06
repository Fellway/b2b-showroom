package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.Guard;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.Translations;

import java.util.Objects;

/** @param parentId {@code null} dla kategorii głównej */
public record CategoryDetails(CategoryId parentId, String slug, int sortOrder, boolean visible,
                              Translations<CategoryTranslation> translations) {

    public CategoryDetails {
        slug = Guard.slug(slug, 120, "catalog.category.slug");
        Objects.requireNonNull(translations, "translations");
        if (sortOrder < 0) {
            throw new InvalidValueException("catalog.sort-order", "Sort order must not be negative");
        }
    }
}
