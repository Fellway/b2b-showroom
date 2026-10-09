package pl.wizytowka.catalog.adapter.in.web.dto;

import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.ColorSwatchResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.ImageResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.PriceResponse;
import pl.wizytowka.catalog.application.view.AdminProductListItem;
import pl.wizytowka.catalog.application.view.AdminProductView;
import pl.wizytowka.catalog.domain.ProductStatus;
import pl.wizytowka.catalog.domain.ProductTranslation;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdminProductResponses {

    private AdminProductResponses() {
    }

    public record ProductListItemResponse(UUID id,
                                          String slug,
                                          String styleCode,
                                          String name,
                                          String categoryName,
                                          ProductStatus status,
                                          PriceResponse price,
                                          int colorCount,
                                          int variantCount,
                                          int activeVariantCount,
                                          List<String> missingLanguages,
                                          ImageResponse cover,
                                          Instant updatedAt,
                                          long version) {

        public static ProductListItemResponse of(AdminProductListItem item) {
            return new ProductListItemResponse(item.id(), item.slug(), item.styleCode(), item.name(), item.categoryName(),
                    item.status(), PriceResponse.of(item.price()), item.colorCount(), item.variantCount(),
                    item.activeVariantCount(), item.missingLanguages().stream().map(CatalogLanguage::code).toList(),
                    ImageResponse.of(item.cover()), item.updatedAt(), item.version());
        }
    }

    /** @param translations klucz = kod języka ("pl", "en", "cs", "uk") */
    public record ProductResponse(UUID id,
                                  UUID categoryId,
                                  String slug,
                                  String styleCode,
                                  ProductStatus status,
                                  PriceResponse price,
                                  boolean featured,
                                  int sortOrder,
                                  Instant publishedAt,
                                  Map<String, TranslationResponse> translations,
                                  List<ColorSwatchResponse> colors,
                                  List<VariantResponse> variants,
                                  List<ImageResponse> images,
                                  Instant createdAt,
                                  Instant updatedAt,
                                  long version) {

        public static ProductResponse of(AdminProductView view) {
            return new ProductResponse(view.id(), view.categoryId(), view.slug(), view.styleCode(), view.status(),
                    PriceResponse.of(view.price()), view.featured(), view.sortOrder(), view.publishedAt(),
                    LanguageMaps.toCodes(view.translations(), TranslationResponse::of),
                    view.colors().stream().map(ColorSwatchResponse::of).toList(),
                    view.variants().stream().map(VariantResponse::of).toList(),
                    view.images().stream().map(ImageResponse::of).toList(),
                    view.createdAt(), view.updatedAt(), view.version());
        }
    }

    public record TranslationResponse(String name, String description, String materialComposition,
                                      String careInstructions) {

        static TranslationResponse of(ProductTranslation translation) {
            return new TranslationResponse(translation.name(), translation.description(),
                    translation.materialComposition(), translation.careInstructions());
        }
    }

    public record VariantResponse(UUID id, UUID colorId, UUID sizeId, String size, String sku, String ean, boolean active) {

        static VariantResponse of(AdminProductView.Variant variant) {
            return new VariantResponse(variant.id(), variant.colorId(), variant.sizeId(), variant.sizeCode(),
                    variant.sku(), variant.ean(), variant.active());
        }
    }
}
