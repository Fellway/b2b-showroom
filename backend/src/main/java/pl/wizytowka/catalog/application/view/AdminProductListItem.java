package pl.wizytowka.catalog.application.view;

import pl.wizytowka.catalog.domain.ProductStatus;

import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Wiersz listy produktów w panelu; nazwy w języku bazowym.
 * @param missingLanguages języki bez tłumaczenia (tłumaczenia są wklejane z zewnątrz — panel pokazuje braki)
 */
public record AdminProductListItem(UUID id,
                                   String slug,
                                   String styleCode,
                                   String name,
                                   String categoryName,
                                   ProductStatus status,
                                   PriceView price,
                                   int colorCount,
                                   int variantCount,
                                   int activeVariantCount,
                                   List<CatalogLanguage> missingLanguages,
                                   ImageView cover,
                                   Instant updatedAt,
                                   long version) {
}
