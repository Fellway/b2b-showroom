package pl.wizytowka.catalog.domain;

import org.junit.jupiter.api.Test;
import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.DomainException;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.ResourceConflictException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wizytowka.catalog.domain.CatalogTestData.EAN_1;
import static pl.wizytowka.catalog.domain.CatalogTestData.EAN_2;
import static pl.wizytowka.catalog.domain.CatalogTestData.EAN_3;
import static pl.wizytowka.catalog.domain.CatalogTestData.image;

class ProductTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private final ColorId navy = ColorId.newId();
    private final ColorId black = ColorId.newId();
    private final SizeId sizeS = SizeId.newId();
    private final SizeId sizeM = SizeId.newId();

    @Test
    void newProductIsDraftWithNormalizedCodes() {
        Product product = Product.create(CatalogTestData.details(CategoryId.newId(), "koszula", " ls-001 "));

        assertThat(product.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.styleCode()).isEqualTo("LS-001");
        assertThat(product.id().value().version()).isEqualTo(7);
    }

    @Test
    void addingVariantAddsItsColorOnce() {
        Product product = CatalogTestData.product();

        product.addVariant(navy, sizeS, Sku.of("ls1-nav-s"), Gtin.of(EAN_1));
        product.addVariant(navy, sizeM, Sku.of("LS1-NAV-M"), Gtin.of(EAN_2));

        assertThat(product.colors()).containsExactly(navy);
        assertThat(product.variants()).extracting(v -> v.sku().value()).containsExactly("LS1-NAV-S", "LS1-NAV-M");
    }

    @Test
    void rejectsDuplicateSizeSkuAndEanWithinProduct() {
        Product product = CatalogTestData.product();
        product.addVariant(navy, sizeS, Sku.of("A"), Gtin.of(EAN_1));

        assertCode(() -> product.addVariant(navy, sizeS, Sku.of("B"), Gtin.of(EAN_2)), "catalog.variant.size-duplicate");
        assertCode(() -> product.addVariant(black, sizeS, Sku.of("A"), Gtin.of(EAN_2)), "catalog.variant.sku-taken");
        assertCode(() -> product.addVariant(black, sizeS, Sku.of("B"), Gtin.of(EAN_1)), "catalog.variant.ean-taken");
    }

    @Test
    void publishingRequiresActiveVariantAndKeepsFirstPublicationDate() {
        Product product = CatalogTestData.product();
        assertCode(() -> product.publish(NOW), "catalog.product.publish-without-variants");

        product.addVariant(navy, sizeS, Sku.of("A"), Gtin.of(EAN_1));
        product.publish(NOW);
        product.hide();
        product.publish(NOW.plusSeconds(60));

        assertThat(product.status()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(product.publishedAt()).isEqualTo(NOW);
    }

    @Test
    void archivedProductMustBeRestoredBeforePublishing() {
        Product product = CatalogTestData.product();
        product.addVariant(navy, sizeS, Sku.of("A"), Gtin.of(EAN_1));
        product.archive();

        assertCode(() -> product.publish(NOW), "catalog.product.publish-archived");
        product.restoreToDraft();
        product.publish(NOW);
        assertThat(product.isPubliclyVisible()).isTrue();
    }

    @Test
    void publishedProductKeepsAtLeastOneActiveVariant() {
        Product product = CatalogTestData.product();
        VariantId only = product.addVariant(navy, sizeS, Sku.of("A"), Gtin.of(EAN_1));
        product.publish(NOW);

        assertCode(() -> product.updateVariant(only, Sku.of("A"), Gtin.of(EAN_1), false), "catalog.product.last-active-variant");
        assertCode(() -> product.removeVariant(only), "catalog.product.last-active-variant");
        assertCode(() -> product.removeColor(navy), "catalog.product.last-active-variant");

        product.addVariant(black, sizeS, Sku.of("B"), Gtin.of(EAN_2));
        product.removeColor(navy);
        assertThat(product.variants()).extracting(v -> v.sku().value()).containsExactly("B");
    }

    @Test
    void removingColorRemovesItsVariantsAndImagesAndReturnsFiles() {
        Product product = CatalogTestData.product();
        product.addVariant(navy, sizeS, Sku.of("A"), Gtin.of(EAN_1));
        product.addVariant(black, sizeS, Sku.of("B"), Gtin.of(EAN_2));
        product.addImage(ImageId.newId(), image("products/p/general.webp"), null);
        product.addImage(ImageId.newId(), image("products/p/navy.webp"), navy);

        List<StoredImage> removed = product.removeColor(navy);

        assertThat(removed).extracting(StoredImage::storageKey).containsExactly("products/p/navy.webp");
        assertThat(product.colors()).containsExactly(black);
        assertThat(product.images()).extracting(i -> i.file().storageKey()).containsExactly("products/p/general.webp");
    }

    @Test
    void imageColorMustBelongToProduct() {
        Product product = CatalogTestData.product();

        assertThatThrownBy(() -> product.addImage(ImageId.newId(), image("products/p/x.webp"), navy))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void coverImagePrefersColorImageThenFirstImage() {
        Product product = CatalogTestData.product();
        product.addColor(navy);
        product.addColor(black);
        ImageId general = ImageId.newId();
        ImageId navyImage = ImageId.newId();
        product.addImage(general, image("products/p/general.webp"), null);
        product.addImage(navyImage, image("products/p/navy.webp"), navy);

        assertThat(product.coverImage(navy)).get().extracting(ProductImage::id).isEqualTo(navyImage);
        assertThat(product.coverImage(black)).get().extracting(ProductImage::id).isEqualTo(general);

        product.reorderImages(List.of(navyImage, general));
        assertThat(product.coverImage(null)).get().extracting(ProductImage::id).isEqualTo(navyImage);
    }

    @Test
    void reorderingRequiresCompleteList() {
        Product product = CatalogTestData.product();
        product.addColor(navy);
        product.addColor(black);

        assertThatThrownBy(() -> product.reorderColors(List.of(navy))).isInstanceOf(InvalidValueException.class);
        product.reorderColors(List.of(black, navy));
        assertThat(product.colors()).containsExactly(black, navy);
    }

    @Test
    void addingSameColorTwiceIsConflict() {
        Product product = CatalogTestData.product();
        product.addColor(navy);

        assertThatThrownBy(() -> product.addColor(navy)).isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void hidingRequiresPublishedProduct() {
        assertThatThrownBy(() -> CatalogTestData.product().hide()).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void restoreRejectsChildrenReferencingUnknownColor() {
        ProductVariant orphan = ProductVariant.restore(VariantId.newId(), navy, sizeS, Sku.of("A"), Gtin.of(EAN_3), true);
        Product template = CatalogTestData.product();

        assertThatThrownBy(() -> Product.restore(template.id(), template.details(), ProductStatus.DRAFT, null,
                List.of(), List.of(orphan), List.of(), 0)).isInstanceOf(ResourceNotFoundException.class);
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).code()).isEqualTo(code);
    }
}
