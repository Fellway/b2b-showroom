package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.Pricing;
import pl.wizytowka.shared.domain.ResourceConflictException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;
import pl.wizytowka.shared.domain.Translations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Korzeń agregatu katalogu: Produkt -> Kolor -> Wariant (rozmiar + SKU/EAN), plus zdjęcia.
 * Wszystkie zmiany dzieci przechodzą przez ten obiekt, który pilnuje niezmienników:
 * <ul>
 *   <li>kolor występuje w produkcie najwyżej raz, a para kolor x rozmiar — najwyżej raz,</li>
 *   <li>SKU i EAN są unikalne w obrębie produktu (unikalność globalną sprawdza warstwa aplikacji),</li>
 *   <li>zdjęcie koloru może wskazywać tylko kolor obecny w produkcie,</li>
 *   <li>opublikowany produkt ma zawsze co najmniej jeden aktywny wariant.</li>
 * </ul>
 */
public final class Product {

    private final ProductId id;
    private CategoryId categoryId;
    private String slug;
    private String styleCode;
    private Pricing pricing;
    private boolean featured;
    private int sortOrder;
    private Translations<ProductTranslation> translations;
    private ProductStatus status;
    private Instant publishedAt;
    private final List<ColorId> colors;
    private final List<ProductVariant> variants;
    private final List<ProductImage> images;
    private final long version;

    private Product(ProductId id, ProductDetails details, ProductStatus status, Instant publishedAt,
                    List<ColorId> colors, List<ProductVariant> variants, List<ProductImage> images, long version) {
        this.id = Objects.requireNonNull(id, "id");
        applyDetails(details);
        this.status = Objects.requireNonNull(status, "status");
        this.publishedAt = publishedAt;
        this.colors = new ArrayList<>(colors);
        this.variants = new ArrayList<>(variants);
        this.images = new ArrayList<>(images);
        this.version = version;
    }

    public static Product create(ProductDetails details) {
        return new Product(ProductId.newId(), details, ProductStatus.DRAFT, null, List.of(), List.of(), List.of(), 0);
    }

    /** Odtworzenie stanu z magazynu danych — bez reguł tworzenia, ale z kontrolą spójności dzieci. */
    public static Product restore(ProductId id, ProductDetails details, ProductStatus status, Instant publishedAt,
                                  List<ColorId> colors, List<ProductVariant> variants, List<ProductImage> images,
                                  long version) {
        Product product = new Product(id, details, status, publishedAt, colors, variants, images, version);
        product.variants.forEach(v -> product.requireColor(v.colorId()));
        product.images.stream().map(ProductImage::colorId).filter(Objects::nonNull).forEach(product::requireColor);
        return product;
    }

    // ------------------------------------------------------------------ dane podstawowe

    public void updateDetails(ProductDetails details) {
        applyDetails(details);
    }

    private void applyDetails(ProductDetails details) {
        Objects.requireNonNull(details, "details");
        this.categoryId = details.categoryId();
        this.slug = details.slug();
        this.styleCode = details.styleCode();
        this.pricing = details.pricing();
        this.featured = details.featured();
        this.sortOrder = details.sortOrder();
        this.translations = details.translations();
    }

    public ProductTranslation translation(CatalogLanguage language) {
        return translations.resolve(language);
    }

    // ------------------------------------------------------------------ cykl życia

    public void publish(Instant now) {
        switch (status) {
            case PUBLISHED -> {
                return;
            }
            case ARCHIVED -> throw new BusinessRuleViolationException("catalog.product.publish-archived",
                    "Archived product must be restored to draft before publishing");
            case DRAFT, HIDDEN -> requireActiveVariant("catalog.product.publish-without-variants");
        }
        status = ProductStatus.PUBLISHED;
        if (publishedAt == null) {
            publishedAt = Objects.requireNonNull(now, "now");
        }
    }

    public void hide() {
        if (status != ProductStatus.PUBLISHED) {
            throw new BusinessRuleViolationException("catalog.product.hide-not-published",
                    "Only a published product can be hidden");
        }
        status = ProductStatus.HIDDEN;
    }

    public void archive() {
        status = ProductStatus.ARCHIVED;
    }

    public void restoreToDraft() {
        if (status != ProductStatus.ARCHIVED) {
            throw new BusinessRuleViolationException("catalog.product.restore-not-archived",
                    "Only an archived product can be restored");
        }
        status = ProductStatus.DRAFT;
    }

    public boolean isPubliclyVisible() {
        return status == ProductStatus.PUBLISHED;
    }

    // ------------------------------------------------------------------ kolory

    public void addColor(ColorId colorId) {
        Objects.requireNonNull(colorId, "colorId");
        if (colors.contains(colorId)) {
            throw new ResourceConflictException("catalog.product.color-duplicate", "Color already added to product");
        }
        colors.add(colorId);
    }

    /** Usuwa kolor wraz z jego wariantami i zdjęciami; zwraca zdjęcia do usunięcia z magazynu plików. */
    public List<StoredImage> removeColor(ColorId colorId) {
        requireColor(colorId);
        List<ProductVariant> remainingVariants = variants.stream().filter(v -> !v.colorId().equals(colorId)).toList();
        if (status == ProductStatus.PUBLISHED && remainingVariants.stream().noneMatch(ProductVariant::isActive)) {
            throw lastActiveVariantViolation();
        }
        List<StoredImage> removedFiles = images.stream()
                .filter(image -> colorId.equals(image.colorId()))
                .map(ProductImage::file)
                .toList();
        variants.retainAll(remainingVariants);
        images.removeIf(image -> colorId.equals(image.colorId()));
        colors.remove(colorId);
        return removedFiles;
    }

    public void reorderColors(List<ColorId> ordered) {
        if (ordered.size() != colors.size() || !new HashSet<>(ordered).equals(new HashSet<>(colors))) {
            throw new InvalidValueException("catalog.product.color-order", "Order must list every product color exactly once");
        }
        colors.clear();
        colors.addAll(ordered);
    }

    // ------------------------------------------------------------------ warianty

    /** Dodaje wariant; kolor zostaje dołączony do produktu automatycznie, jeśli go jeszcze nie ma. */
    public VariantId addVariant(ColorId colorId, SizeId sizeId, Sku sku, Gtin ean) {
        Objects.requireNonNull(sizeId, "sizeId");
        if (!colors.contains(colorId)) {
            addColor(colorId);
        }
        boolean sizeTaken = variants.stream().anyMatch(v -> v.colorId().equals(colorId) && v.sizeId().equals(sizeId));
        if (sizeTaken) {
            throw new ResourceConflictException("catalog.variant.size-duplicate", "This color already has a variant in this size");
        }
        requireUniqueCodes(sku, ean, null);
        ProductVariant variant = ProductVariant.create(colorId, sizeId, sku, ean);
        variants.add(variant);
        return variant.id();
    }

    public void updateVariant(VariantId variantId, Sku sku, Gtin ean, boolean active) {
        ProductVariant variant = variant(variantId);
        requireUniqueCodes(sku, ean, variantId);
        if (!active && status == ProductStatus.PUBLISHED && isOnlyActiveVariant(variant)) {
            throw lastActiveVariantViolation();
        }
        variant.update(sku, ean, active);
    }

    public void removeVariant(VariantId variantId) {
        ProductVariant variant = variant(variantId);
        if (status == ProductStatus.PUBLISHED && isOnlyActiveVariant(variant)) {
            throw lastActiveVariantViolation();
        }
        variants.remove(variant);
    }

    public ProductVariant variant(VariantId variantId) {
        return variants.stream()
                .filter(v -> v.id().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("catalog.variant.not-found", "Variant not found in product"));
    }

    private boolean isOnlyActiveVariant(ProductVariant variant) {
        return variant.isActive() && variants.stream().filter(ProductVariant::isActive).count() == 1;
    }

    private void requireUniqueCodes(Sku sku, Gtin ean, VariantId excluded) {
        for (ProductVariant other : variants) {
            if (other.id().equals(excluded)) {
                continue;
            }
            if (other.sku().equals(sku)) {
                throw new ResourceConflictException("catalog.variant.sku-taken", "SKU already used: " + sku);
            }
            if (other.ean().equals(ean)) {
                throw new ResourceConflictException("catalog.variant.ean-taken", "EAN already used: " + ean);
            }
        }
    }

    private void requireActiveVariant(String code) {
        if (variants.stream().noneMatch(ProductVariant::isActive)) {
            throw new BusinessRuleViolationException(code, "Product needs at least one active variant");
        }
    }

    private static BusinessRuleViolationException lastActiveVariantViolation() {
        return new BusinessRuleViolationException("catalog.product.last-active-variant",
                "Published product must keep at least one active variant; hide the product first");
    }

    // ------------------------------------------------------------------ zdjęcia

    public void addImage(ImageId imageId, StoredImage file, ColorId colorId) {
        if (colorId != null) {
            requireColor(colorId);
        }
        images.add(ProductImage.create(imageId, colorId, file));
    }

    /** @return plik do usunięcia z magazynu (po zatwierdzeniu transakcji) */
    public StoredImage removeImage(ImageId imageId) {
        ProductImage image = image(imageId);
        images.remove(image);
        return image.file();
    }

    public void assignImageColor(ImageId imageId, ColorId colorId) {
        if (colorId != null) {
            requireColor(colorId);
        }
        image(imageId).assignColor(colorId);
    }

    /** Ustala kolejność zdjęć; pierwsze staje się zdjęciem głównym produktu. */
    public void reorderImages(List<ImageId> ordered) {
        List<ImageId> current = images.stream().map(ProductImage::id).toList();
        if (ordered.size() != current.size() || !new HashSet<>(ordered).equals(new HashSet<>(current))) {
            throw new InvalidValueException("catalog.product.image-order", "Order must list every product image exactly once");
        }
        List<ProductImage> reordered = ordered.stream().map(this::image).toList();
        images.clear();
        images.addAll(reordered);
    }

    /** Pierwsze zdjęcie danego koloru, a w razie jego braku — główne zdjęcie produktu. */
    public Optional<ProductImage> coverImage(ColorId colorId) {
        return images.stream()
                .filter(image -> colorId != null && colorId.equals(image.colorId()))
                .findFirst()
                .or(() -> images.stream().findFirst());
    }

    private ProductImage image(ImageId imageId) {
        return images.stream()
                .filter(image -> image.id().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("catalog.image.not-found", "Image not found in product"));
    }

    private void requireColor(ColorId colorId) {
        if (!colors.contains(colorId)) {
            throw new ResourceNotFoundException("catalog.product.color-not-found", "Color is not part of this product");
        }
    }

    // ------------------------------------------------------------------ odczyt

    public ProductId id() {
        return id;
    }

    public ProductDetails details() {
        return new ProductDetails(categoryId, slug, styleCode, pricing, featured, sortOrder, translations);
    }

    public CategoryId categoryId() {
        return categoryId;
    }

    public String slug() {
        return slug;
    }

    public String styleCode() {
        return styleCode;
    }

    public Pricing pricing() {
        return pricing;
    }

    public Translations<ProductTranslation> translations() {
        return translations;
    }

    public ProductStatus status() {
        return status;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    public List<ColorId> colors() {
        return Collections.unmodifiableList(colors);
    }

    public List<ProductVariant> variants() {
        return Collections.unmodifiableList(variants);
    }

    public List<ProductImage> images() {
        return Collections.unmodifiableList(images);
    }

    public long version() {
        return version;
    }
}
