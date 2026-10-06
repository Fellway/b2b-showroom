package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.wizytowka.catalog.application.port.out.CategoryRepository;
import pl.wizytowka.catalog.application.port.out.ColorRepository;
import pl.wizytowka.catalog.application.port.out.ProductRepository;
import pl.wizytowka.catalog.application.port.out.SizeRepository;
import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.Product;
import pl.wizytowka.catalog.domain.ProductImage;
import pl.wizytowka.catalog.domain.ProductStatus;
import pl.wizytowka.catalog.domain.ProductVariant;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.catalog.domain.Sku;
import pl.wizytowka.catalog.domain.StoredImage;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.support.PersistenceTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PersistenceTest
class ProductPersistenceAdapterTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-10-05T10:00:00Z");

    @Autowired
    ProductRepository products;
    @Autowired
    CategoryRepository categories;
    @Autowired
    ColorRepository colors;
    @Autowired
    SizeRepository sizes;
    @Autowired
    ProductJpaRepository productJpaRepository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    JdbcTemplate jdbc;

    CatalogFixture fixture;
    CategoryId shirts;
    ColorId navy;
    ColorId black;
    SizeId sizeS;
    SizeId sizeM;

    @BeforeEach
    void setUp() {
        fixture = new CatalogFixture(categories, colors, sizes, products);
        shirts = fixture.category("koszule", null, true, "Koszule");
        navy = fixture.color("NAVY", "#1C2340", "Granatowy", "Navy");
        black = fixture.color("BLK", "#111111", "Czarny", null);
        sizeS = fixture.size("S", 20);
        sizeM = fixture.size("M", 30);
    }

    @Test
    void roundTripsWholeAggregate() {
        Product product = fixture.product(shirts, "koszula-lniana", "LS-001")
                .name(CatalogLanguage.EN, "Linen shirt")
                .featured()
                .color(black)
                .variant(navy, sizeM)
                .variant(navy, sizeS)
                .inactiveVariant(black, sizeM)
                .image("products/ls-001/hero.webp", null)
                .image("products/ls-001/black.webp", black)
                .published(PUBLISHED_AT)
                .save();
        flushAndClear();

        Product loaded = products.findById(product.id()).orElseThrow();

        assertThat(loaded.version()).isZero();
        assertThat(loaded.status()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(loaded.publishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(loaded.details()).isEqualTo(product.details());
        assertThat(loaded.translation(CatalogLanguage.EN).name()).isEqualTo("Linen shirt");
        assertThat(loaded.colors()).containsExactly(black, navy);
        assertThat(loaded.variants())
                .extracting(v -> v.id(), ProductVariant::colorId, ProductVariant::sizeId, ProductVariant::sku,
                        ProductVariant::ean, ProductVariant::isActive)
                .containsExactlyInAnyOrderElementsOf(product.variants().stream()
                        .map(v -> org.assertj.core.groups.Tuple.tuple(v.id(), v.colorId(), v.sizeId(), v.sku(), v.ean(), v.isActive()))
                        .toList());
        assertThat(loaded.images()).extracting(ProductImage::id).containsExactlyElementsOf(
                product.images().stream().map(ProductImage::id).toList());
        assertThat(loaded.images()).extracting(ProductImage::colorId).containsExactly(null, black);
        assertThat(loaded.images().getFirst().file()).isEqualTo(product.images().getFirst().file());
    }

    @Test
    void reorderingIsPersistedAndBumpsVersionOnce() {
        Product product = fixture.product(shirts, "koszula", "LS-002")
                .variant(navy, sizeM).variant(black, sizeM)
                .image("p/a.webp", null).image("p/b.webp", navy)
                .save();
        flushAndClear();

        Product loaded = products.findById(product.id()).orElseThrow();
        loaded.reorderColors(List.of(black, navy));
        loaded.reorderImages(loaded.images().reversed().stream().map(ProductImage::id).toList());
        products.save(loaded);
        flushAndClear();

        Product reloaded = products.findById(product.id()).orElseThrow();
        assertThat(reloaded.colors()).containsExactly(black, navy);
        assertThat(reloaded.images()).extracting(i -> i.file().storageKey()).containsExactly("p/b.webp", "p/a.webp");
        assertThat(reloaded.version()).isEqualTo(1);
    }

    @Test
    void childOnlyChangeIncrementsAggregateVersion() {
        Product product = fixture.product(shirts, "koszula", "LS-003").variant(navy, sizeM).save();
        flushAndClear();

        Product loaded = products.findById(product.id()).orElseThrow();
        loaded.addVariant(navy, sizeS, Sku.of("LS003-NEW"), Gtin.of(fixture.nextEan()));
        products.save(loaded);
        flushAndClear();

        Product reloaded = products.findById(product.id()).orElseThrow();
        assertThat(reloaded.version()).isEqualTo(1);
        assertThat(reloaded.variants()).hasSize(2);
    }

    @Test
    void rejectsSaveOfStaleAggregate() {
        Product product = fixture.product(shirts, "koszula", "LS-004").variant(navy, sizeM).save();
        flushAndClear();
        Product first = products.findById(product.id()).orElseThrow();
        Product stale = products.findById(product.id()).orElseThrow();

        first.addColor(black);
        products.save(first);
        productJpaRepository.flush();

        stale.addColor(black);
        assertThatThrownBy(() -> products.save(stale)).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void databaseRejectsDuplicateEanAcrossProducts() {
        String sharedEan = fixture.nextEan();
        Product first = fixture.product(shirts, "pierwsza", "A-1").build();
        first.addVariant(navy, sizeM, Sku.of("A1-M"), Gtin.of(sharedEan));
        products.save(first);
        productJpaRepository.flush();

        Product second = fixture.product(shirts, "druga", "B-1").build();
        second.addVariant(navy, sizeM, Sku.of("B1-M"), Gtin.of(sharedEan));
        products.save(second);

        assertThatThrownBy(() -> productJpaRepository.flush()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void imageColorForeignKeyRejectsColorOfAnotherProduct() {
        Product first = fixture.product(shirts, "pierwsza", "A-1").variant(navy, sizeM).save();
        Product second = fixture.product(shirts, "druga", "B-1").variant(black, sizeM).save();
        productJpaRepository.flush();
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");

        assertThatThrownBy(() -> jdbc.update("""
                insert into product_image (id, product_id, color_id, storage_key, content_type, sort_order, created_at)
                values (gen_random_uuid(), ?, ?, 'x/y.webp', 'image/webp', 0, now())
                """, second.id().value(), navy.value()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(first.colors()).contains(navy);
    }

    @Test
    void removingColorRemovesItsVariantsAndImages() {
        Product product = fixture.product(shirts, "koszula", "LS-005")
                .variant(navy, sizeM).variant(navy, sizeS).variant(black, sizeM)
                .image("p/navy.webp", navy).image("p/general.webp", null)
                .save();
        flushAndClear();

        Product loaded = products.findById(product.id()).orElseThrow();
        List<StoredImage> removedFiles = loaded.removeColor(navy);
        products.save(loaded);
        flushAndClear();
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");

        assertThat(removedFiles).extracting(StoredImage::storageKey).containsExactly("p/navy.webp");
        assertThat(count("product_color", product)).isEqualTo(1);
        assertThat(count("product_variant", product)).isEqualTo(1);
        assertThat(count("product_image", product)).isEqualTo(1);
        assertThat(products.findById(product.id()).orElseThrow().colors()).containsExactly(black);
    }

    @Test
    void deletesProductWithAllChildren() {
        Product product = fixture.product(shirts, "koszula", "LS-006")
                .name(CatalogLanguage.EN, "Shirt")
                .variant(navy, sizeM).image("p/x.webp", navy)
                .save();
        flushAndClear();

        products.delete(products.findById(product.id()).orElseThrow());
        flushAndClear();
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");

        assertThat(products.findById(product.id())).isEmpty();
        for (String table : List.of("product_translation", "product_color", "product_variant", "product_image")) {
            assertThat(count(table, product)).as(table).isZero();
        }
    }

    @Test
    void answersUniquenessAndUsageQuestions() {
        Product product = fixture.product(shirts, "koszula", "LS-007").build();
        String ean = fixture.nextEan();
        product.addVariant(navy, sizeM, Sku.of("LS007-M"), Gtin.of(ean));
        products.save(product);
        flushAndClear();
        Product other = fixture.product(shirts, "inna", "LS-008").build();

        assertThat(products.existsBySlug("koszula", null)).isTrue();
        assertThat(products.existsBySlug("koszula", product.id())).isFalse();
        assertThat(products.existsBySlug("koszula", other.id())).isTrue();
        assertThat(products.existsByStyleCode("LS-007", other.id())).isTrue();
        assertThat(products.existsByStyleCode("LS-007", product.id())).isFalse();
        assertThat(products.existsBySkuInOtherProduct(Sku.of("ls007-m"), other.id())).isTrue();
        assertThat(products.existsBySkuInOtherProduct(Sku.of("LS007-M"), product.id())).isFalse();
        assertThat(products.existsByEanInOtherProduct(Gtin.of(ean), other.id())).isTrue();
        assertThat(products.existsByEanInOtherProduct(Gtin.of(ean), null)).isTrue();
        assertThat(products.existsByEanInOtherProduct(Gtin.of(ean), product.id())).isFalse();
        assertThat(products.existsByCategory(shirts)).isTrue();
        assertThat(products.existsByColor(navy)).isTrue();
        assertThat(products.existsByColor(black)).isFalse();
        assertThat(products.existsBySize(sizeM)).isTrue();
        assertThat(products.existsBySize(sizeS)).isFalse();
    }

    private long count(String table, Product product) {
        return jdbc.queryForObject("select count(*) from " + table + " where product_id = ?", Long.class, product.id().value());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
