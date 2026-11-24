package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.catalog.application.port.out.CatalogReadModel;
import pl.wizytowka.catalog.application.view.CatalogFacetsView;
import pl.wizytowka.catalog.application.view.CatalogFilter;
import pl.wizytowka.catalog.application.view.CategoryView;
import pl.wizytowka.catalog.application.view.ColorSwatchView;
import pl.wizytowka.catalog.application.view.PriceView;
import pl.wizytowka.catalog.application.view.ProductCardView;
import pl.wizytowka.catalog.application.view.ProductDetailView;
import pl.wizytowka.catalog.application.view.ProductLabel;
import pl.wizytowka.catalog.application.view.ProductPricingView;
import pl.wizytowka.catalog.application.view.VariantSnapshot;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.ProductStatus;
import pl.wizytowka.catalog.domain.Sku;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Publiczna strona odczytu katalogu. Filtrowanie i stronicowanie wykonuje baza (JPQL zwracający
 * stronę identyfikatorów), a widoki są składane w pamięci z encji doładowanych wsadowo.
 */
@Repository
@Transactional(readOnly = true)
class CatalogReadModelAdapter implements CatalogReadModel {

    private final EntityManager entityManager;
    private final CatalogDictionaryLoader dictionaryLoader;

    CatalogReadModelAdapter(EntityManager entityManager, CatalogDictionaryLoader dictionaryLoader) {
        this.entityManager = entityManager;
        this.dictionaryLoader = dictionaryLoader;
    }

    // ------------------------------------------------------------------ lista produktów

    @Override
    public PageResult<ProductCardView> findPublishedProducts(CatalogFilter filter, CatalogLanguage language, PageQuery page) {
        CatalogDictionary dictionary = dictionaryLoader.load();
        Set<UUID> categoryIds = categoryScope(filter.categorySlug(), dictionary);
        if (categoryIds.isEmpty()) {
            return new PageResult<>(List.of(), page.page(), page.size(), 0);
        }

        List<String> predicates = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        predicates.add("p.status = :published");
        parameters.put("published", ProductStatus.PUBLISHED);
        predicates.add("p.categoryId in :categoryIds");
        parameters.put("categoryIds", categoryIds);
        if (!filter.colorIds().isEmpty()) {
            predicates.add("exists (select 1 from ProductVariantJpaEntity v where v.product = p and v.active = true"
                    + " and v.colorId in :colorIds)");
            parameters.put("colorIds", filter.colorIds());
        }
        if (!filter.sizeIds().isEmpty()) {
            predicates.add("exists (select 1 from ProductVariantJpaEntity v where v.product = p and v.active = true"
                    + " and v.sizeId in :sizeIds)");
            parameters.put("sizeIds", filter.sizeIds());
        }
        if (filter.search() != null) {
            predicates.add("(lower(p.styleCode) like :search escape '!'"
                    + " or exists (select 1 from ProductJpaEntity p2 join p2.translations t"
                    + " where p2 = p and key(t) in :searchLanguages and lower(t.name) like :search escape '!'))");
            parameters.put("search", CatalogDictionary.containsPattern(filter.search()));
            parameters.put("searchLanguages", EnumSet.of(language, CatalogLanguage.DEFAULT));
        }
        if (filter.featuredOnly()) {
            predicates.add("p.featured = true");
        }
        String where = " where " + String.join(" and ", predicates);
        String orderBy = switch (filter.sort()) {
            case RECOMMENDED -> " order by p.featured desc, p.sortOrder asc, p.publishedAt desc, p.id asc";
            case NEWEST -> " order by p.publishedAt desc, p.id desc";
            case PRICE_ASC -> " order by p.wholesalePrice asc, p.id asc";
            case PRICE_DESC -> " order by p.wholesalePrice desc, p.id asc";
        };

        TypedQuery<Long> countQuery = entityManager.createQuery("select count(p) from ProductJpaEntity p" + where, Long.class);
        TypedQuery<UUID> idQuery = entityManager.createQuery("select p.id from ProductJpaEntity p" + where + orderBy, UUID.class);
        parameters.forEach((name, value) -> {
            countQuery.setParameter(name, value);
            idQuery.setParameter(name, value);
        });
        long total = countQuery.getSingleResult();
        List<UUID> ids = idQuery.setFirstResult((int) page.offset()).setMaxResults(page.size()).getResultList();

        List<ProductCardView> cards = loadInOrder(entityManager, ids).stream()
                .map(product -> card(product, dictionary, language))
                .toList();
        return new PageResult<>(cards, page.page(), page.size(), total);
    }

    /** Zakres kategorii dla filtra: widoczne kategorie, zawężone do wybranej kategorii i jej potomków. */
    private static Set<UUID> categoryScope(String categorySlug, CatalogDictionary dictionary) {
        Set<UUID> visible = dictionary.visibleCategoryIds();
        if (categorySlug == null) {
            return visible;
        }
        return dictionary.categoryBySlug(categorySlug)
                .filter(category -> visible.contains(category.getId()))
                .map(category -> dictionary.withDescendants(category.getId()).stream()
                        .filter(visible::contains)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private ProductCardView card(ProductJpaEntity product, CatalogDictionary dictionary, CatalogLanguage language) {
        List<ProductVariantJpaEntity> active = product.getVariants().stream().filter(ProductVariantJpaEntity::isActive).toList();
        Set<UUID> activeColors = active.stream().map(ProductVariantJpaEntity::getColorId).collect(Collectors.toSet());
        List<ColorSwatchView> swatches = CatalogDictionary.sortedColorIds(product).stream()
                .filter(activeColors::contains)
                .map(colorId -> dictionary.swatch(colorId, language))
                .toList();
        List<String> sizes = active.stream()
                .map(ProductVariantJpaEntity::getSizeId)
                .distinct()
                .sorted(dictionary.sizeOrder())
                .map(dictionary::sizeCode)
                .toList();
        return new ProductCardView(
                product.getId(),
                product.getSlug(),
                product.getStyleCode(),
                ProductEntityMapper.translation(product, language).name(),
                dictionary.categorySlug(product.getCategoryId()),
                PriceView.of(ProductEntityMapper.pricing(product)),
                product.isFeatured(),
                CatalogDictionary.sortedImages(product).stream().findFirst().map(CatalogDictionary::image).orElse(null),
                swatches,
                sizes);
    }

    // ------------------------------------------------------------------ karta produktu

    @Override
    public Optional<ProductDetailView> findPublishedProduct(String slug, CatalogLanguage language) {
        List<ProductJpaEntity> found = entityManager.createQuery(
                        "select p from ProductJpaEntity p where p.slug = :slug and p.status = :published", ProductJpaEntity.class)
                .setParameter("slug", slug)
                .setParameter("published", ProductStatus.PUBLISHED)
                .getResultList();
        if (found.isEmpty()) {
            return Optional.empty();
        }
        CatalogDictionary dictionary = dictionaryLoader.load();
        ProductJpaEntity product = found.getFirst();
        if (!dictionary.visibleCategoryIds().contains(product.getCategoryId())) {
            return Optional.empty();
        }
        return Optional.of(detail(product, dictionary, language));
    }

    private ProductDetailView detail(ProductJpaEntity product, CatalogDictionary dictionary, CatalogLanguage language) {
        Map<UUID, List<ProductVariantJpaEntity>> activeByColor = product.getVariants().stream()
                .filter(ProductVariantJpaEntity::isActive)
                .collect(Collectors.groupingBy(ProductVariantJpaEntity::getColorId));
        List<ProductDetailView.ColorOption> colors = CatalogDictionary.sortedColorIds(product).stream()
                .filter(activeByColor::containsKey)
                .map(colorId -> {
                    ColorSwatchView swatch = dictionary.swatch(colorId, language);
                    List<ProductDetailView.VariantOption> variants = activeByColor.get(colorId).stream()
                            .sorted((a, b) -> dictionary.sizeOrder().compare(a.getSizeId(), b.getSizeId()))
                            .map(v -> new ProductDetailView.VariantOption(v.getId(), dictionary.sizeCode(v.getSizeId()),
                                    v.getSku(), v.getEan()))
                            .toList();
                    return new ProductDetailView.ColorOption(colorId, swatch.code(), swatch.name(), swatch.hex(), variants);
                })
                .toList();
        CategoryJpaEntity category = dictionary.category(product.getCategoryId()).orElseThrow();
        ProductTranslationEmbeddable translation = ProductEntityMapper.translation(product, language);
        return new ProductDetailView(
                product.getId(),
                product.getSlug(),
                product.getStyleCode(),
                translation.name(),
                translation.description(),
                translation.materialComposition(),
                translation.careInstructions(),
                new ProductDetailView.CategoryRef(category.getId(), category.getSlug(),
                        dictionary.categoryName(category.getId(), language)),
                PriceView.of(ProductEntityMapper.pricing(product)),
                colors,
                CatalogDictionary.images(product));
    }

    // ------------------------------------------------------------------ kategorie i filtry

    @Override
    public List<CategoryView> findVisibleCategories(CatalogLanguage language) {
        CatalogDictionary dictionary = dictionaryLoader.load();
        Set<UUID> visible = dictionary.visibleCategoryIds();
        return dictionary.categoriesSorted().stream()
                .filter(category -> visible.contains(category.getId()))
                .map(category -> {
                    CategoryTranslationEmbeddable translation = CatalogDictionary.resolve(category.getTranslations(), language);
                    return new CategoryView(category.getId(), category.getParentId(), category.getSlug(),
                            translation.name(), translation.description(), category.getSortOrder());
                })
                .toList();
    }

    @Override
    public CatalogFacetsView findFacets(CatalogLanguage language) {
        CatalogDictionary dictionary = dictionaryLoader.load();
        Set<UUID> visible = dictionary.visibleCategoryIds();
        if (visible.isEmpty()) {
            return new CatalogFacetsView(List.of(), List.of());
        }
        Map<UUID, Long> colorCounts = facetCounts("v.colorId", visible);
        Map<UUID, Long> sizeCounts = facetCounts("v.sizeId", visible);
        List<CatalogFacetsView.ColorFacet> colors = dictionary.colorsSorted().stream()
                .filter(color -> colorCounts.containsKey(color.getId()))
                .map(color -> new CatalogFacetsView.ColorFacet(color.getId(), color.getCode(),
                        dictionary.colorName(color.getId(), language), color.getHexValue(), colorCounts.get(color.getId())))
                .toList();
        List<CatalogFacetsView.SizeFacet> sizes = dictionary.sizesSorted().stream()
                .filter(size -> sizeCounts.containsKey(size.getId()))
                .map(size -> new CatalogFacetsView.SizeFacet(size.getId(), size.getCode(), sizeCounts.get(size.getId())))
                .toList();
        return new CatalogFacetsView(colors, sizes);
    }

    /** @param attribute stała nazwa atrybutu ("v.colorId" / "v.sizeId") — nigdy dane wejściowe użytkownika */
    private Map<UUID, Long> facetCounts(String attribute, Set<UUID> visibleCategories) {
        List<Object[]> rows = entityManager.createQuery(
                        "select " + attribute + ", count(distinct p.id) from ProductVariantJpaEntity v join v.product p"
                                + " where v.active = true and p.status = :published and p.categoryId in :categoryIds"
                                + " group by " + attribute, Object[].class)
                .setParameter("published", ProductStatus.PUBLISHED)
                .setParameter("categoryIds", visibleCategories)
                .getResultList();
        return CatalogDictionary.counts(rows);
    }

    // ------------------------------------------------------------------ ceny dla kupców (D23)

    @Override
    public List<ProductPricingView> findPublishedPricing(Collection<ProductId> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> ids = productIds.stream().map(ProductId::value).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, ProductJpaEntity> found = entityManager.createQuery(
                        "select p from ProductJpaEntity p where p.id in :ids and p.status = :published", ProductJpaEntity.class)
                .setParameter("ids", ids)
                .setParameter("published", ProductStatus.PUBLISHED)
                .getResultStream()
                .collect(Collectors.toMap(ProductJpaEntity::getId, Function.identity()));
        if (found.isEmpty()) {
            return List.of();
        }
        CatalogDictionary dictionary = dictionaryLoader.load();
        Set<UUID> visibleCategories = dictionary.visibleCategoryIds();
        return ids.stream()
                .map(found::get)
                .filter(java.util.Objects::nonNull)
                .filter(product -> visibleCategories.contains(product.getCategoryId()))
                .map(product -> {
                    // kolejność jak na karcie produktu: kolory produktu, w nich rozmiarówka
                    List<UUID> colorOrder = CatalogDictionary.sortedColorIds(product);
                    return new ProductPricingView(
                            product.getId(),
                            PriceView.of(ProductEntityMapper.pricing(product)),
                            product.getVariants().stream()
                                    .filter(ProductVariantJpaEntity::isActive)
                                    .sorted(Comparator.comparingInt((ProductVariantJpaEntity v) -> colorOrder.indexOf(v.getColorId()))
                                            .thenComparing(ProductVariantJpaEntity::getSizeId, dictionary.sizeOrder()))
                                    .map(v -> new ProductPricingView.VariantCode(v.getId(), v.getSku(), v.getEan()))
                                    .toList());
                })
                .toList();
    }

    // ------------------------------------------------------------------ porty dla innych modułów

    @Override
    public List<VariantSnapshot> findSelectableVariants(Collection<VariantId> variantIds, CatalogLanguage language) {
        if (variantIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> ids = variantIds.stream().map(VariantId::value).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, ProductVariantJpaEntity> found = entityManager.createQuery(
                        "select v from ProductVariantJpaEntity v join fetch v.product p"
                                + " where v.id in :ids and v.active = true and p.status = :published",
                        ProductVariantJpaEntity.class)
                .setParameter("ids", ids)
                .setParameter("published", ProductStatus.PUBLISHED)
                .getResultStream()
                .collect(Collectors.toMap(ProductVariantJpaEntity::getId, Function.identity()));
        if (found.isEmpty()) {
            return List.of();
        }
        CatalogDictionary dictionary = dictionaryLoader.load();
        Set<UUID> visibleCategories = dictionary.visibleCategoryIds();
        return ids.stream()
                .map(found::get)
                .filter(java.util.Objects::nonNull)
                // produkt w ukrytej kategorii jest niewidoczny dla klienta, więc nie może trafić do zestawienia
                .filter(variant -> visibleCategories.contains(variant.getProduct().getCategoryId()))
                .map(variant -> {
                    ProductJpaEntity product = variant.getProduct();
                    return new VariantSnapshot(
                            VariantId.of(variant.getId()),
                            ProductId.of(product.getId()),
                            product.getStyleCode(),
                            ProductEntityMapper.translation(product, language).name(),
                            dictionary.colorName(variant.getColorId(), language),
                            dictionary.sizeCode(variant.getSizeId()),
                            Sku.of(variant.getSku()),
                            Gtin.of(variant.getEan()),
                            ProductEntityMapper.pricing(product),
                            CatalogDictionary.coverImage(product, variant.getColorId())
                                    .map(ProductImageJpaEntity::getStorageKey)
                                    .orElse(null));
                })
                .toList();
    }

    @Override
    public Map<ProductId, ProductLabel> findProductLabels(Collection<ProductId> productIds, CatalogLanguage language) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Set<UUID> ids = productIds.stream().map(ProductId::value).collect(Collectors.toSet());
        Map<ProductId, ProductLabel> labels = new LinkedHashMap<>();
        for (ProductJpaEntity product : entityManager.createQuery(
                        "select p from ProductJpaEntity p where p.id in :ids", ProductJpaEntity.class)
                .setParameter("ids", ids)
                .getResultList()) {
            labels.put(ProductId.of(product.getId()), new ProductLabel(product.getId(), product.getSlug(),
                    product.getStyleCode(), ProductEntityMapper.translation(product, language).name(), product.getStatus()));
        }
        return labels;
    }

    /** Ładuje produkty po id, zachowując kolejność wejścia (wynik stronicowanego zapytania o id). */
    static List<ProductJpaEntity> loadInOrder(EntityManager entityManager, List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, ProductJpaEntity> byId = entityManager.createQuery(
                        "select p from ProductJpaEntity p where p.id in :ids", ProductJpaEntity.class)
                .setParameter("ids", ids)
                .getResultStream()
                .collect(Collectors.toMap(ProductJpaEntity::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }
}
