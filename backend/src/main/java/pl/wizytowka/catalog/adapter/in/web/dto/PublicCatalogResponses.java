package pl.wizytowka.catalog.adapter.in.web.dto;

import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.ColorSwatchResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.ImageResponse;
import pl.wizytowka.catalog.application.view.CatalogFacetsView;
import pl.wizytowka.catalog.application.view.CategoryView;
import pl.wizytowka.catalog.application.view.ProductCardView;
import pl.wizytowka.catalog.application.view.ProductDetailView;

import java.util.List;
import java.util.UUID;

/**
 * Odpowiedzi publicznego katalogu (treści w języku żądania). Celowo bez cen i kodów EAN (D23) — te dostaje
 * wyłącznie zalogowany kupiec z {@code /api/v1/buyer/catalog/pricing}.
 */
public final class PublicCatalogResponses {

    private PublicCatalogResponses() {
    }

    public record ProductCardResponse(UUID id,
                                      String slug,
                                      String styleCode,
                                      String name,
                                      String categorySlug,
                                      boolean featured,
                                      ImageResponse cover,
                                      List<ColorSwatchResponse> colors,
                                      List<String> sizes) {

        public static ProductCardResponse of(ProductCardView view) {
            return new ProductCardResponse(view.id(), view.slug(), view.styleCode(), view.name(), view.categorySlug(),
                    view.featured(), ImageResponse.of(view.cover()),
                    view.colors().stream().map(ColorSwatchResponse::of).toList(), List.copyOf(view.sizes()));
        }
    }

    public record ProductDetailResponse(UUID id,
                                        String slug,
                                        String styleCode,
                                        String name,
                                        String description,
                                        String materialComposition,
                                        String careInstructions,
                                        CategoryRefResponse category,
                                        List<ColorOptionResponse> colors,
                                        List<ImageResponse> images) {

        public static ProductDetailResponse of(ProductDetailView view) {
            ProductDetailView.CategoryRef category = view.category();
            return new ProductDetailResponse(view.id(), view.slug(), view.styleCode(), view.name(), view.description(),
                    view.materialComposition(), view.careInstructions(),
                    category == null ? null : new CategoryRefResponse(category.id(), category.slug(), category.name()),
                    view.colors().stream().map(ColorOptionResponse::of).toList(),
                    view.images().stream().map(ImageResponse::of).toList());
        }
    }

    public record CategoryRefResponse(UUID id, String slug, String name) {
    }

    public record ColorOptionResponse(UUID id, String code, String name, String hex, List<VariantOptionResponse> variants) {

        static ColorOptionResponse of(ProductDetailView.ColorOption option) {
            return new ColorOptionResponse(option.colorId(), option.code(), option.name(), option.hex(),
                    option.variants().stream()
                            .map(v -> new VariantOptionResponse(v.id(), v.sizeCode(), v.sku()))
                            .toList());
        }
    }

    public record VariantOptionResponse(UUID id, String size, String sku) {
    }

    public record CategoryResponse(UUID id, UUID parentId, String slug, String name, String description, int sortOrder) {

        public static CategoryResponse of(CategoryView view) {
            return new CategoryResponse(view.id(), view.parentId(), view.slug(), view.name(), view.description(),
                    view.sortOrder());
        }
    }

    public record FacetsResponse(List<ColorFacetResponse> colors, List<SizeFacetResponse> sizes) {

        public static FacetsResponse of(CatalogFacetsView view) {
            return new FacetsResponse(
                    view.colors().stream()
                            .map(c -> new ColorFacetResponse(c.colorId(), c.code(), c.name(), c.hex(), c.productCount()))
                            .toList(),
                    view.sizes().stream()
                            .map(s -> new SizeFacetResponse(s.sizeId(), s.code(), s.productCount()))
                            .toList());
        }
    }

    public record ColorFacetResponse(UUID id, String code, String name, String hex, long productCount) {
    }

    public record SizeFacetResponse(UUID id, String code, long productCount) {
    }
}
