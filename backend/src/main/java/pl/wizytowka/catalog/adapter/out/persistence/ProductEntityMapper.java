package pl.wizytowka.catalog.adapter.out.persistence;

import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.ImageId;
import pl.wizytowka.catalog.domain.Product;
import pl.wizytowka.catalog.domain.ProductDetails;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.ProductImage;
import pl.wizytowka.catalog.domain.ProductTranslation;
import pl.wizytowka.catalog.domain.ProductVariant;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.catalog.domain.Sku;
import pl.wizytowka.catalog.domain.StoredImage;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.Pricing;
import pl.wizytowka.shared.domain.Translations;

import java.util.Comparator;
import java.util.Currency;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mapowanie agregatu {@link Product} na model persystencji i z powrotem. Kolekcje dzieci są synchronizowane
 * po identyfikatorach na zarządzanych kolekcjach Hibernate (nigdy nie są podmieniane).
 */
final class ProductEntityMapper {

    private ProductEntityMapper() {
    }

    static Product toDomain(ProductJpaEntity entity) {
        List<ColorId> colors = entity.getColors().stream()
                .sorted(Comparator.comparingInt(ProductColorJpaEntity::getSortOrder))
                .map(color -> ColorId.of(color.getColorId()))
                .toList();
        List<ProductVariant> variants = entity.getVariants().stream()
                .map(v -> ProductVariant.restore(VariantId.of(v.getId()), ColorId.of(v.getColorId()), SizeId.of(v.getSizeId()),
                        Sku.of(v.getSku()), Gtin.of(v.getEan()), v.isActive()))
                .toList();
        List<ProductImage> images = entity.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImageJpaEntity::getSortOrder))
                .map(image -> ProductImage.restore(ImageId.of(image.getId()),
                        image.getColorId() == null ? null : ColorId.of(image.getColorId()),
                        new StoredImage(image.getStorageKey(), image.getContentType(), image.getWidthPx(),
                                image.getHeightPx(), image.getSizeBytes())))
                .toList();
        return Product.restore(ProductId.of(entity.getId()), details(entity), entity.getStatus(), entity.getPublishedAt(),
                colors, variants, images, entity.getVersion());
    }

    static ProductDetails details(ProductJpaEntity entity) {
        return new ProductDetails(CategoryId.of(entity.getCategoryId()), entity.getSlug(), entity.getStyleCode(),
                pricing(entity), entity.isFeatured(), entity.getSortOrder(), Translations.of(translations(entity)));
    }

    static Pricing pricing(ProductJpaEntity entity) {
        return new Pricing(entity.getWholesalePrice(), entity.getRetailPrice(), Currency.getInstance(entity.getCurrency()));
    }

    static Map<CatalogLanguage, ProductTranslation> translations(ProductJpaEntity entity) {
        Map<CatalogLanguage, ProductTranslation> result = new EnumMap<>(CatalogLanguage.class);
        entity.getTranslations().forEach((language, t) -> result.put(language,
                new ProductTranslation(t.name(), t.description(), t.materialComposition(), t.careInstructions())));
        return result;
    }

    /** Treść w żądanym języku z fallbackiem do języka bazowego. */
    static ProductTranslationEmbeddable translation(ProductJpaEntity entity, CatalogLanguage language) {
        ProductTranslationEmbeddable exact = entity.getTranslations().get(language);
        return exact != null ? exact : entity.getTranslations().get(CatalogLanguage.DEFAULT);
    }

    static void apply(Product product, ProductJpaEntity entity) {
        ProductDetails details = product.details();
        entity.setCategoryId(details.categoryId().value());
        entity.setSlug(details.slug());
        entity.setStyleCode(details.styleCode());
        entity.setWholesalePrice(details.pricing().wholesalePrice());
        entity.setRetailPrice(details.pricing().retailPrice());
        entity.setCurrency(details.pricing().currency().getCurrencyCode());
        entity.setFeatured(details.featured());
        entity.setSortOrder(details.sortOrder());
        entity.setStatus(product.status());
        entity.setPublishedAt(product.publishedAt());
        syncTranslations(product.translations().asMap(), entity.getTranslations());
        syncColors(product.colors(), entity);
        syncVariants(product.variants(), entity);
        syncImages(product.images(), entity);
    }

    private static void syncTranslations(Map<CatalogLanguage, ProductTranslation> desired,
                                         Map<CatalogLanguage, ProductTranslationEmbeddable> current) {
        current.keySet().retainAll(desired.keySet());
        desired.forEach((language, t) -> {
            ProductTranslationEmbeddable value = new ProductTranslationEmbeddable(
                    t.name(), t.description(), t.materialComposition(), t.careInstructions());
            if (!value.equals(current.get(language))) {
                current.put(language, value);
            }
        });
    }

    private static void syncColors(List<ColorId> desired, ProductJpaEntity entity) {
        Set<UUID> desiredIds = desired.stream().map(ColorId::value).collect(Collectors.toSet());
        List<ProductColorJpaEntity> colors = entity.getColors();
        colors.removeIf(color -> !desiredIds.contains(color.getColorId()));
        Map<UUID, ProductColorJpaEntity> existing = colors.stream()
                .collect(Collectors.toMap(ProductColorJpaEntity::getColorId, Function.identity()));
        for (int i = 0; i < desired.size(); i++) {
            UUID colorId = desired.get(i).value();
            ProductColorJpaEntity color = existing.get(colorId);
            if (color == null) {
                colors.add(new ProductColorJpaEntity(entity, colorId, i));
            } else if (color.getSortOrder() != i) {
                color.setSortOrder(i);
            }
        }
        colors.sort(Comparator.comparingInt(ProductColorJpaEntity::getSortOrder));
    }

    private static void syncVariants(List<ProductVariant> desired, ProductJpaEntity entity) {
        Map<UUID, ProductVariant> desiredById = desired.stream()
                .collect(Collectors.toMap(v -> v.id().value(), Function.identity()));
        List<ProductVariantJpaEntity> variants = entity.getVariants();
        variants.removeIf(v -> !desiredById.containsKey(v.getId()));
        Set<UUID> existingIds = new HashSet<>();
        for (ProductVariantJpaEntity variant : variants) {
            ProductVariant source = desiredById.get(variant.getId());
            existingIds.add(variant.getId());
            if (!source.sku().value().equals(variant.getSku()) || !source.ean().value().equals(variant.getEan())
                    || source.isActive() != variant.isActive()) {
                variant.update(source.sku().value(), source.ean().value(), source.isActive());
            }
        }
        for (ProductVariant source : desired) {
            if (!existingIds.contains(source.id().value())) {
                ProductVariantJpaEntity variant = new ProductVariantJpaEntity(source.id().value(), entity,
                        source.colorId().value(), source.sizeId().value());
                variant.update(source.sku().value(), source.ean().value(), source.isActive());
                variants.add(variant);
            }
        }
    }

    private static void syncImages(List<ProductImage> desired, ProductJpaEntity entity) {
        Map<UUID, Integer> positions = new HashMap<>();
        for (int i = 0; i < desired.size(); i++) {
            positions.put(desired.get(i).id().value(), i);
        }
        List<ProductImageJpaEntity> images = entity.getImages();
        images.removeIf(image -> !positions.containsKey(image.getId()));
        Map<UUID, ProductImageJpaEntity> existing = images.stream()
                .collect(Collectors.toMap(ProductImageJpaEntity::getId, Function.identity()));
        for (ProductImage source : desired) {
            UUID colorId = source.colorId() == null ? null : source.colorId().value();
            int position = positions.get(source.id().value());
            ProductImageJpaEntity image = existing.get(source.id().value());
            if (image == null) {
                StoredImage file = source.file();
                image = new ProductImageJpaEntity(source.id().value(), entity, file.storageKey(), file.contentType(),
                        file.widthPx(), file.heightPx(), file.sizeBytes());
                images.add(image);
            }
            if (!Objects.equals(image.getColorId(), colorId)) {
                image.setColorId(colorId);
            }
            if (image.getSortOrder() != position) {
                image.setSortOrder(position);
            }
        }
        images.sort(Comparator.comparingInt(ProductImageJpaEntity::getSortOrder));
    }
}
