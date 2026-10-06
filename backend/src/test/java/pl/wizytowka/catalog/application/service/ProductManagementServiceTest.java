package pl.wizytowka.catalog.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.wizytowka.catalog.application.port.in.StatusTransition;
import pl.wizytowka.catalog.application.port.out.ImageInspector.InspectedImage;
import pl.wizytowka.catalog.domain.CatalogTestData;
import pl.wizytowka.catalog.domain.Category;
import pl.wizytowka.catalog.domain.CategoryDetails;
import pl.wizytowka.catalog.domain.CategoryTranslation;
import pl.wizytowka.catalog.domain.Color;
import pl.wizytowka.catalog.domain.ColorDetails;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.ImageId;
import pl.wizytowka.catalog.domain.Product;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.ProductStatus;
import pl.wizytowka.catalog.domain.Size;
import pl.wizytowka.catalog.domain.Sku;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.shared.domain.DomainException;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.ResourceConflictException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;
import pl.wizytowka.shared.domain.Translations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wizytowka.catalog.domain.CatalogTestData.EAN_1;
import static pl.wizytowka.catalog.domain.CatalogTestData.EAN_2;

class ProductManagementServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private static final byte[] PNG_BYTES = {1, 2, 3};

    private final InMemoryCatalogRepositories repositories = new InMemoryCatalogRepositories();
    private final RecordingFileStorage storage = new RecordingFileStorage();
    private InspectedImage inspectionResult = new InspectedImage("image/png", "png", 800, 1000);

    private ProductManagementService products;
    private ProductImageService images;
    private CategoryManagementService categories;
    private Category shirts;
    private Color navy;
    private Size sizeM;

    @BeforeEach
    void setUp() {
        products = new ProductManagementService(repositories.productRepository, repositories.categoryRepository,
                repositories.colorRepository, repositories.sizeRepository, storage, storage,
                Clock.fixed(NOW, ZoneOffset.UTC));
        images = new ProductImageService(repositories.productRepository, content -> Optional.ofNullable(inspectionResult),
                storage, storage);
        categories = new CategoryManagementService(repositories.categoryRepository, repositories.productRepository);

        shirts = Category.create(new CategoryDetails(null, "koszule", 0, true,
                Translations.ofDefault(new CategoryTranslation("Koszule", null))));
        navy = Color.create(new ColorDetails("NAVY", "#1c2340", 0, Translations.ofDefault("Granatowy")));
        sizeM = Size.create("M", 30);
        repositories.categoryRepository.save(shirts);
        repositories.colorRepository.save(navy);
        repositories.sizeRepository.save(sizeM);
    }

    @Test
    void createsProductInExistingCategory() {
        ProductId id = products.create(CatalogTestData.details(shirts.id(), "koszula", "LS-1"));

        assertThat(repositories.products.get(id).status()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    void rejectsUnknownCategoryAndTakenSlugOrStyleCode() {
        products.create(CatalogTestData.details(shirts.id(), "koszula", "LS-1"));

        assertCode(() -> products.create(CatalogTestData.details(
                pl.wizytowka.catalog.domain.CategoryId.newId(), "inna", "LS-2")), "catalog.category.not-found");
        assertCode(() -> products.create(CatalogTestData.details(shirts.id(), "koszula", "LS-2")), "catalog.product.slug-taken");
        assertCode(() -> products.create(CatalogTestData.details(shirts.id(), "inna", "ls-1")), "catalog.product.style-code-taken");
    }

    @Test
    void updateKeepsOwnSlugButRejectsStaleVersion() {
        ProductId id = products.create(CatalogTestData.details(shirts.id(), "koszula", "LS-1"));

        products.update(id, 0, CatalogTestData.details(shirts.id(), "koszula", "LS-1"));
        assertThatThrownBy(() -> products.update(id, 7, CatalogTestData.details(shirts.id(), "koszula", "LS-1")))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void variantCodesMustBeGloballyUnique() {
        ProductId first = products.create(CatalogTestData.details(shirts.id(), "pierwsza", "A-1"));
        ProductId second = products.create(CatalogTestData.details(shirts.id(), "druga", "B-1"));
        products.addVariant(first, navy.id(), sizeM.id(), Sku.of("A1-M"), Gtin.of(EAN_1));

        assertThatThrownBy(() -> products.addVariant(second, navy.id(), sizeM.id(), Sku.of("A1-M"), Gtin.of(EAN_2)))
                .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> products.addVariant(second, navy.id(), sizeM.id(), Sku.of("B1-M"), Gtin.of(EAN_1)))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void variantRequiresExistingDictionaryEntries() {
        ProductId id = products.create(CatalogTestData.details(shirts.id(), "koszula", "LS-1"));

        assertCode(() -> products.addVariant(id, pl.wizytowka.catalog.domain.ColorId.newId(), sizeM.id(),
                Sku.of("X"), Gtin.of(EAN_1)), "catalog.color.not-found");
        assertCode(() -> products.addVariant(id, navy.id(), pl.wizytowka.catalog.domain.SizeId.newId(),
                Sku.of("X"), Gtin.of(EAN_1)), "catalog.size.not-found");
    }

    @Test
    void publishesWithClockTime() {
        ProductId id = products.create(CatalogTestData.details(shirts.id(), "koszula", "LS-1"));
        products.addVariant(id, navy.id(), sizeM.id(), Sku.of("LS1-M"), Gtin.of(EAN_1));

        products.changeStatus(id, 0, StatusTransition.PUBLISH);

        Product product = repositories.products.get(id);
        assertThat(product.status()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(product.publishedAt()).isEqualTo(NOW);
    }

    @Test
    void uploadStoresFileUnderServerGeneratedKeyAndCleansUpOnRollback() {
        ProductId id = products.create(CatalogTestData.details(shirts.id(), "koszula", "LS-1"));

        ImageId imageId = images.upload(id, null, PNG_BYTES);

        String key = "products/%s/%s.png".formatted(id, imageId);
        assertThat(storage.files).containsKey(key);
        assertThat(repositories.products.get(id).images()).singleElement()
                .satisfies(image -> assertThat(image.file().storageKey()).isEqualTo(key));
        storage.rollback();
        assertThat(storage.files).doesNotContainKey(key);
    }

    @Test
    void uploadRejectsUnrecognizedContentAndUnknownColorBeforeStoring() {
        ProductId id = products.create(CatalogTestData.details(shirts.id(), "koszula", "LS-1"));

        assertThatThrownBy(() -> images.upload(id, navy.id(), PNG_BYTES)).isInstanceOf(ResourceNotFoundException.class);
        inspectionResult = null;
        assertCode(() -> images.upload(id, null, PNG_BYTES), "catalog.image.unsupported-format");
        assertThatThrownBy(() -> images.upload(id, null, new byte[0])).isInstanceOf(InvalidValueException.class);
        assertThat(storage.files).isEmpty();
    }

    @Test
    void deletingProductRemovesFilesOnlyAfterCommit() {
        ProductId id = products.create(CatalogTestData.details(shirts.id(), "koszula", "LS-1"));
        images.upload(id, null, PNG_BYTES);
        storage.commit();

        products.delete(id);
        assertThat(storage.files).hasSize(1);
        storage.commit();

        assertThat(storage.files).isEmpty();
        assertThat(repositories.products).doesNotContainKey(id);
    }

    @Test
    void removingVariantKeepsColor() {
        ProductId id = products.create(CatalogTestData.details(shirts.id(), "koszula", "LS-1"));
        VariantId variantId = products.addVariant(id, navy.id(), sizeM.id(), Sku.of("LS1-M"), Gtin.of(EAN_1));

        products.removeVariant(id, variantId);

        assertThat(repositories.products.get(id).colors()).containsExactly(navy.id());
    }

    @Test
    void categoryCannotBeMovedUnderItsDescendant() {
        var child = categories.create(new CategoryDetails(shirts.id(), "lniane", 0, true,
                Translations.ofDefault(new CategoryTranslation("Lniane", null))));

        assertCode(() -> categories.update(shirts.id(), 0, new CategoryDetails(child, "koszule", 0, true,
                Translations.ofDefault(new CategoryTranslation("Koszule", null)))), "catalog.category.cycle");
        assertCode(() -> categories.delete(shirts.id()), "catalog.category.in-use");
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).code()).isEqualTo(code);
    }
}
