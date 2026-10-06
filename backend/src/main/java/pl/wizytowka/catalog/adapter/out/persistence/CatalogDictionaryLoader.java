package pl.wizytowka.catalog.adapter.out.persistence;

import org.springframework.stereotype.Component;

@Component
class CatalogDictionaryLoader {

    private final CategoryJpaRepository categories;
    private final ColorJpaRepository colors;
    private final SizeJpaRepository sizes;

    CatalogDictionaryLoader(CategoryJpaRepository categories, ColorJpaRepository colors, SizeJpaRepository sizes) {
        this.categories = categories;
        this.colors = colors;
        this.sizes = sizes;
    }

    CatalogDictionary load() {
        return new CatalogDictionary(categories.findAll(), colors.findAll(), sizes.findAll());
    }
}
