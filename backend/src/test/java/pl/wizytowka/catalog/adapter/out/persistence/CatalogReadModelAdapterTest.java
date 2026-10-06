package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.wizytowka.catalog.application.port.out.AdminCatalogReadModel;
import pl.wizytowka.catalog.application.port.out.CatalogReadModel;
import pl.wizytowka.catalog.application.port.out.CategoryRepository;
import pl.wizytowka.catalog.application.port.out.ColorRepository;
import pl.wizytowka.catalog.application.port.out.ProductRepository;
import pl.wizytowka.catalog.application.port.out.SizeRepository;
import pl.wizytowka.catalog.application.view.AdminProductFilter;
import pl.wizytowka.catalog.application.view.AdminProductListItem;
import pl.wizytowka.catalog.application.view.AdminProductView;
import pl.wizytowka.catalog.application.view.CatalogFacetsView;
import pl.wizytowka.catalog.application.view.CatalogFilter;
import pl.wizytowka.catalog.application.view.CatalogSort;
import pl.wizytowka.catalog.application.view.CategoryView;
import pl.wizytowka.catalog.application.view.ColorSwatchView;
import pl.wizytowka.catalog.application.view.ProductCardView;
import pl.wizytowka.catalog.application.view.ProductDetailView;
import pl.wizytowka.catalog.application.view.VariantSnapshot;
import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.Product;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.ProductStatus;
import pl.wizytowka.catalog.domain.ProductVariant;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.support.PersistenceTest;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@PersistenceTest
class CatalogReadModelAdapterTest {

    private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");
    private static final PageQuery FIRST_PAGE = new PageQuery(0, 20);

    @Autowired
    CatalogReadModel readModel;
    @Autowired
    AdminCatalogReadModel adminReadModel;
    @Autowired
    ProductRepository products;
    @Autowired
    CategoryRepository categories;
    @Autowired
    ColorRepository colors;
    @Autowired
    SizeRepository sizes;
    @Autowired
    EntityManager entityManager;

    CategoryId clothing;
    CategoryId shirts;
    CategoryId oldStuff;
    ColorId navy;
    ColorId black;
    ColorId red;
    SizeId sizeS;
    SizeId sizeM;
    SizeId sizeL;
    Product shirt;
    Product trousers;
    Product draft;
    Product hidden;
    Product inHiddenCategory;

    @BeforeEach
    void setUp() {
        CatalogFixture fixture = new CatalogFixture(categories, colors, sizes, products);
        clothing = fixture.category("odziez", null, true, "Odzież");
        shirts = fixture.category("koszule", clothing, true, "Koszule");
        CategoryId archive = fixture.category("archiwum", null, false, "Archiwum");
        oldStuff = fixture.category("stare", archive, true, "Stare");
        navy = fixture.color("NAVY", "#1C2340", "Granatowy", "Navy");
        black = fixture.color("BLK", "#111111", "Czarny", null);
        red = fixture.color("RED", "#FF0000", "Czerwony", null);
        sizeL = fixture.size("L", 40);
        sizeS = fixture.size("S", 20);
        sizeM = fixture.size("M", 30);

        shirt = fixture.product(shirts, "koszula-lniana", "LS-001")
                .name(CatalogLanguage.EN, "Linen shirt")
                .price("149.00", "349.99")
                .featured()
                .variant(navy, sizeM).variant(navy, sizeS)
                .inactiveVariant(black, sizeL)
                .image("products/ls-001/hero.webp", null)
                .image("products/ls-001/navy.webp", navy)
                .published(T0)
                .save();
        trousers = fixture.product(clothing, "spodnie-100", "TR-100")
                .name(CatalogLanguage.PL, "Spodnie 100% bawełna")
                .price("99.00", "229.00")
                .variant(black, sizeM)
                .published(T0.plusSeconds(3600))
                .save();
        draft = fixture.product(shirts, "szkic", "DR-001").variant(navy, sizeM).save();
        hidden = fixture.product(shirts, "ukryty", "HD-001").variant(navy, sizeM).published(T0).hidden().save();
        inHiddenCategory = fixture.product(oldStuff, "stara-koszula", "OLD-001").variant(red, sizeM).published(T0).save();
        entityManager.flush();
        entityManager.clear();
    }

    // ------------------------------------------------------------------ lista publiczna

    @Test
    void listsOnlyVisiblePublishedProductsInRecommendedOrder() {
        PageResult<ProductCardView> page = readModel.findPublishedProducts(CatalogFilter.none(), CatalogLanguage.PL, FIRST_PAGE);

        assertThat(page.totalItems()).isEqualTo(2);
        assertThat(page.items()).extracting(ProductCardView::slug).containsExactly("koszula-lniana", "spodnie-100");
    }

    @Test
    void buildsCardsInRequestedLanguageWithActiveColorsAndSortedSizes() {
        ProductCardView card = readModel.findPublishedProducts(CatalogFilter.none(), CatalogLanguage.EN, FIRST_PAGE)
                .items().getFirst();

        assertThat(card.name()).isEqualTo("Linen shirt");
        assertThat(card.categorySlug()).isEqualTo("koszule");
        assertThat(card.featured()).isTrue();
        assertThat(card.price().wholesale()).isEqualByComparingTo("149.00");
        assertThat(card.price().currency()).isEqualTo("PLN");
        assertThat(card.cover().storageKey()).isEqualTo("products/ls-001/hero.webp");
        assertThat(card.colors()).extracting(ColorSwatchView::code, ColorSwatchView::name).containsExactly(
                org.assertj.core.groups.Tuple.tuple("NAVY", "Navy"));
        assertThat(card.sizes()).containsExactly("S", "M");

        ProductCardView czech = readModel.findPublishedProducts(CatalogFilter.none(), CatalogLanguage.CS, FIRST_PAGE)
                .items().getFirst();
        assertThat(czech.name()).as("brak CS -> fallback PL").isEqualTo("Produkt LS-001");
    }

    @Test
    void filtersByCategoryIncludingDescendants() {
        assertThat(slugs(filter("odziez", Set.of(), Set.of(), null))).containsExactlyInAnyOrder("koszula-lniana", "spodnie-100");
        assertThat(slugs(filter("koszule", Set.of(), Set.of(), null))).containsExactly("koszula-lniana");
        assertThat(slugs(filter("stare", Set.of(), Set.of(), null))).isEmpty();
        assertThat(slugs(filter("nie-istnieje", Set.of(), Set.of(), null))).isEmpty();
    }

    @Test
    void filtersByActiveVariantColorAndSize() {
        assertThat(slugs(filter(null, Set.of(navy.value()), Set.of(), null))).containsExactly("koszula-lniana");
        assertThat(slugs(filter(null, Set.of(black.value()), Set.of(), null)))
                .as("czarny wariant koszuli jest nieaktywny").containsExactly("spodnie-100");
        assertThat(slugs(filter(null, Set.of(), Set.of(sizeS.value()), null))).containsExactly("koszula-lniana");
        assertThat(slugs(filter(null, Set.of(), Set.of(sizeL.value()), null))).isEmpty();
        assertThat(slugs(filter(null, Set.of(navy.value(), black.value()), Set.of(sizeM.value()), null))).hasSize(2);
    }

    @Test
    void searchesNamesInRequestedAndDefaultLanguageAndStyleCodeWithEscapedWildcards() {
        assertThat(slugs(filter(null, Set.of(), Set.of(), "100%"))).containsExactly("spodnie-100");
        assertThat(slugs(filter(null, Set.of(), Set.of(), "%"))).as("% to literał, nie wildcard").containsExactly("spodnie-100");
        assertThat(slugs(filter(null, Set.of(), Set.of(), "_"))).isEmpty();
        assertThat(slugs(filter(null, Set.of(), Set.of(), "ls-0"))).containsExactly("koszula-lniana");
        assertThat(readModel.findPublishedProducts(new CatalogFilter(null, Set.of(), Set.of(), "LINEN", false, null),
                CatalogLanguage.EN, FIRST_PAGE).items()).extracting(ProductCardView::slug).containsExactly("koszula-lniana");
        assertThat(readModel.findPublishedProducts(new CatalogFilter(null, Set.of(), Set.of(), "produkt ls", false, null),
                CatalogLanguage.EN, FIRST_PAGE).items()).as("nazwa w języku bazowym").hasSize(1);
        assertThat(readModel.findPublishedProducts(new CatalogFilter(null, Set.of(), Set.of(), null, true, null),
                CatalogLanguage.PL, FIRST_PAGE).items()).extracting(ProductCardView::slug).containsExactly("koszula-lniana");
    }

    @Test
    void sortsAndPaginates() {
        assertThat(sorted(CatalogSort.PRICE_ASC)).containsExactly("spodnie-100", "koszula-lniana");
        assertThat(sorted(CatalogSort.PRICE_DESC)).containsExactly("koszula-lniana", "spodnie-100");
        assertThat(sorted(CatalogSort.NEWEST)).containsExactly("spodnie-100", "koszula-lniana");

        PageResult<ProductCardView> second = readModel.findPublishedProducts(CatalogFilter.none(), CatalogLanguage.PL,
                new PageQuery(1, 1));
        assertThat(second.items()).extracting(ProductCardView::slug).containsExactly("spodnie-100");
        assertThat(second.totalItems()).isEqualTo(2);
        assertThat(second.totalPages()).isEqualTo(2);
    }

    // ------------------------------------------------------------------ karta, kategorie, filtry

    @Test
    void returnsDetailWithActiveVariantsOnly() {
        ProductDetailView detail = readModel.findPublishedProduct("koszula-lniana", CatalogLanguage.EN).orElseThrow();

        assertThat(detail.name()).isEqualTo("Linen shirt");
        assertThat(detail.category().slug()).isEqualTo("koszule");
        assertThat(detail.category().name()).isEqualTo("Koszule");
        assertThat(detail.colors()).singleElement().satisfies(color -> {
            assertThat(color.code()).isEqualTo("NAVY");
            assertThat(color.name()).isEqualTo("Navy");
            assertThat(color.variants()).extracting(ProductDetailView.VariantOption::sizeCode).containsExactly("S", "M");
        });
        assertThat(detail.images()).extracting(i -> i.storageKey(), i -> i.colorId()).containsExactly(
                org.assertj.core.groups.Tuple.tuple("products/ls-001/hero.webp", null),
                org.assertj.core.groups.Tuple.tuple("products/ls-001/navy.webp", navy.value()));

        assertThat(readModel.findPublishedProduct("szkic", CatalogLanguage.PL)).isEmpty();
        assertThat(readModel.findPublishedProduct("ukryty", CatalogLanguage.PL)).isEmpty();
        assertThat(readModel.findPublishedProduct("stara-koszula", CatalogLanguage.PL)).isEmpty();
    }

    @Test
    void listsVisibleCategoriesAndFacets() {
        List<CategoryView> visible = readModel.findVisibleCategories(CatalogLanguage.EN);
        assertThat(visible).extracting(CategoryView::slug).containsExactlyInAnyOrder("odziez", "koszule");
        assertThat(visible).filteredOn(c -> c.slug().equals("koszule")).singleElement()
                .satisfies(c -> assertThat(c.parentId()).isEqualTo(clothing.value()));

        CatalogFacetsView facets = readModel.findFacets(CatalogLanguage.PL);
        assertThat(facets.colors()).extracting(CatalogFacetsView.ColorFacet::code, CatalogFacetsView.ColorFacet::productCount)
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple("NAVY", 1L),
                        org.assertj.core.groups.Tuple.tuple("BLK", 1L));
        assertThat(facets.sizes()).extracting(CatalogFacetsView.SizeFacet::code, CatalogFacetsView.SizeFacet::productCount)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("S", 1L), org.assertj.core.groups.Tuple.tuple("M", 2L));
    }

    // ------------------------------------------------------------------ porty dla innych modułów

    @Test
    void returnsSelectableVariantsInInputOrderSkippingUnavailable() {
        VariantId trousersBlackM = variant(trousers, black, sizeM);
        VariantId shirtNavyS = variant(shirt, navy, sizeS);
        VariantId shirtInactive = variant(shirt, black, sizeL);
        VariantId draftVariant = variant(draft, navy, sizeM);
        VariantId hiddenCategoryVariant = variant(inHiddenCategory, red, sizeM);

        List<VariantSnapshot> snapshots = readModel.findSelectableVariants(
                List.of(trousersBlackM, shirtInactive, draftVariant, hiddenCategoryVariant, VariantId.newId(), shirtNavyS),
                CatalogLanguage.EN);

        assertThat(snapshots).extracting(VariantSnapshot::variantId).containsExactly(trousersBlackM, shirtNavyS);
        VariantSnapshot navySnapshot = snapshots.get(1);
        assertThat(navySnapshot.productId()).isEqualTo(shirt.id());
        assertThat(navySnapshot.productName()).isEqualTo("Linen shirt");
        assertThat(navySnapshot.colorName()).isEqualTo("Navy");
        assertThat(navySnapshot.sizeCode()).isEqualTo("S");
        assertThat(navySnapshot.pricing().wholesalePrice()).isEqualByComparingTo("149.00");
        assertThat(navySnapshot.imageStorageKey()).as("zdjęcie koloru").isEqualTo("products/ls-001/navy.webp");
        VariantSnapshot blackSnapshot = snapshots.getFirst();
        assertThat(blackSnapshot.colorName()).as("brak EN -> PL").isEqualTo("Czarny");
        assertThat(blackSnapshot.imageStorageKey()).isNull();
    }

    @Test
    void returnsLabelsForAnyStatusSkippingMissing() {
        var labels = readModel.findProductLabels(List.of(draft.id(), shirt.id(), ProductId.newId()), CatalogLanguage.EN);

        assertThat(labels).containsOnlyKeys(draft.id(), shirt.id());
        assertThat(labels.get(draft.id()).status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(labels.get(shirt.id()).name()).isEqualTo("Linen shirt");
        assertThat(labels.get(shirt.id()).styleCode()).isEqualTo("LS-001");
    }

    // ------------------------------------------------------------------ panel

    @Test
    void adminListIncludesAllStatusesAndSearchesSkuAndEan() {
        PageResult<AdminProductListItem> all = adminReadModel.findProducts(new AdminProductFilter(null, null, null), FIRST_PAGE);
        assertThat(all.totalItems()).isEqualTo(5);
        assertThat(all.items()).isSortedAccordingTo(Comparator.comparing(AdminProductListItem::updatedAt).reversed());
        AdminProductListItem shirtItem = all.items().stream().filter(i -> i.slug().equals("koszula-lniana")).findFirst().orElseThrow();
        assertThat(shirtItem.colorCount()).isEqualTo(2);
        assertThat(shirtItem.variantCount()).isEqualTo(3);
        assertThat(shirtItem.activeVariantCount()).isEqualTo(2);
        assertThat(shirtItem.categoryName()).isEqualTo("Koszule");
        assertThat(shirtItem.cover().storageKey()).isEqualTo("products/ls-001/hero.webp");

        String sku = skuOf(trousers);
        String ean = trousers.variants().getFirst().ean().value();
        assertThat(adminSlugs(new AdminProductFilter(sku.toLowerCase(), null, null))).containsExactly("spodnie-100");
        assertThat(adminSlugs(new AdminProductFilter(ean, null, null))).containsExactly("spodnie-100");
        assertThat(adminSlugs(new AdminProductFilter("linen", null, null))).containsExactly("koszula-lniana");
        assertThat(adminSlugs(new AdminProductFilter(null, ProductStatus.HIDDEN, null))).containsExactly("ukryty");
        assertThat(adminSlugs(new AdminProductFilter(null, null, shirts.value())))
                .containsExactlyInAnyOrder("koszula-lniana", "szkic", "ukryty");
    }

    @Test
    void adminProductViewContainsEverything() {
        AdminProductView view = adminReadModel.findProduct(shirt.id()).orElseThrow();

        assertThat(view.status()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(view.translations()).containsOnlyKeys(CatalogLanguage.PL, CatalogLanguage.EN);
        assertThat(view.colors()).extracting(ColorSwatchView::code).containsExactly("NAVY", "BLK");
        assertThat(view.variants()).extracting(AdminProductView.Variant::sizeCode, AdminProductView.Variant::active)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("S", true), org.assertj.core.groups.Tuple.tuple("M", true),
                        org.assertj.core.groups.Tuple.tuple("L", false));
        assertThat(view.images()).hasSize(2);
        assertThat(view.version()).isZero();
        assertThat(view.createdAt()).isNotNull();
        assertThat(adminReadModel.findProduct(ProductId.newId())).isEmpty();
    }

    @Test
    void adminDictionariesCarryUsageCounts() {
        assertThat(adminReadModel.findCategories())
                .extracting(c -> c.slug(), c -> c.productCount())
                .contains(org.assertj.core.groups.Tuple.tuple("koszule", 3L), org.assertj.core.groups.Tuple.tuple("odziez", 1L),
                        org.assertj.core.groups.Tuple.tuple("archiwum", 0L));
        assertThat(adminReadModel.findColors())
                .extracting(c -> c.code(), c -> c.productCount())
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple("NAVY", 3L),
                        org.assertj.core.groups.Tuple.tuple("BLK", 2L), org.assertj.core.groups.Tuple.tuple("RED", 1L));
        assertThat(adminReadModel.findSizes())
                .extracting(s -> s.code(), s -> s.variantCount())
                .containsExactly(org.assertj.core.groups.Tuple.tuple("S", 1L), org.assertj.core.groups.Tuple.tuple("M", 5L),
                        org.assertj.core.groups.Tuple.tuple("L", 1L));
    }

    // ------------------------------------------------------------------ pomocnicze

    private List<String> slugs(CatalogFilter filter) {
        return readModel.findPublishedProducts(filter, CatalogLanguage.PL, FIRST_PAGE).items().stream()
                .map(ProductCardView::slug).toList();
    }

    private List<String> sorted(CatalogSort sort) {
        return slugs(new CatalogFilter(null, Set.of(), Set.of(), null, false, sort));
    }

    private static CatalogFilter filter(String category, Set<UUID> colorIds, Set<UUID> sizeIds, String search) {
        return new CatalogFilter(category, colorIds, sizeIds, search, false, CatalogSort.RECOMMENDED);
    }

    private List<String> adminSlugs(AdminProductFilter filter) {
        return adminReadModel.findProducts(filter, FIRST_PAGE).items().stream().map(AdminProductListItem::slug).toList();
    }

    private static VariantId variant(Product product, ColorId color, SizeId size) {
        return product.variants().stream()
                .filter(v -> v.colorId().equals(color) && v.sizeId().equals(size))
                .map(ProductVariant::id)
                .findFirst().orElseThrow();
    }

    private static String skuOf(Product product) {
        return product.variants().getFirst().sku().value();
    }
}
