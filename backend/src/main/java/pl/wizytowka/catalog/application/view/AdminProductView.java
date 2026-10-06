package pl.wizytowka.catalog.application.view;

import pl.wizytowka.catalog.domain.ProductStatus;
import pl.wizytowka.catalog.domain.ProductTranslation;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pełny stan produktu do edycji w panelu (wszystkie języki, również nieaktywne warianty). */
public record AdminProductView(UUID id,
                               UUID categoryId,
                               String slug,
                               String styleCode,
                               ProductStatus status,
                               PriceView price,
                               boolean featured,
                               int sortOrder,
                               Instant publishedAt,
                               Map<CatalogLanguage, ProductTranslation> translations,
                               List<ColorSwatchView> colors,
                               List<Variant> variants,
                               List<ImageView> images,
                               Instant createdAt,
                               Instant updatedAt,
                               long version) {

    public record Variant(UUID id, UUID colorId, UUID sizeId, String sizeCode, String sku, String ean, boolean active) {
    }
}
