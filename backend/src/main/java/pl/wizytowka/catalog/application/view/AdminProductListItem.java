package pl.wizytowka.catalog.application.view;

import pl.wizytowka.catalog.domain.ProductStatus;

import java.time.Instant;
import java.util.UUID;

/** Wiersz listy produktów w panelu; nazwy w języku bazowym. */
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
                                   ImageView cover,
                                   Instant updatedAt,
                                   long version) {
}
