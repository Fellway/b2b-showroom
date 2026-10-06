package pl.wizytowka.catalog.application.view;

import pl.wizytowka.catalog.domain.CategoryTranslation;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.Map;
import java.util.UUID;

public record AdminCategoryView(UUID id, UUID parentId, String slug, int sortOrder, boolean visible,
                                Map<CatalogLanguage, CategoryTranslation> translations, long productCount, long version) {
}
