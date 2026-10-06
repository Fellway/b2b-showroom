package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.catalog.application.port.out.AdminCatalogReadModel;
import pl.wizytowka.catalog.application.view.AdminCategoryView;
import pl.wizytowka.catalog.application.view.AdminColorView;
import pl.wizytowka.catalog.application.view.AdminProductFilter;
import pl.wizytowka.catalog.application.view.AdminProductListItem;
import pl.wizytowka.catalog.application.view.AdminProductView;
import pl.wizytowka.catalog.application.view.AdminSizeView;
import pl.wizytowka.catalog.application.view.ColorSwatchView;
import pl.wizytowka.catalog.application.view.PriceView;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Strona odczytu panelu: wszystkie statusy, wszystkie języki, również nieaktywne warianty. */
@Repository
@Transactional(readOnly = true)
class AdminCatalogReadModelAdapter implements AdminCatalogReadModel {

    private static final CatalogLanguage ADMIN_LANGUAGE = CatalogLanguage.DEFAULT;

    private final EntityManager entityManager;
    private final CatalogDictionaryLoader dictionaryLoader;

    AdminCatalogReadModelAdapter(EntityManager entityManager, CatalogDictionaryLoader dictionaryLoader) {
        this.entityManager = entityManager;
        this.dictionaryLoader = dictionaryLoader;
    }

    @Override
    public PageResult<AdminProductListItem> findProducts(AdminProductFilter filter, PageQuery page) {
        List<String> predicates = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        if (filter.status() != null) {
            predicates.add("p.status = :status");
            parameters.put("status", filter.status());
        }
        if (filter.categoryId() != null) {
            predicates.add("p.categoryId = :categoryId");
            parameters.put("categoryId", filter.categoryId());
        }
        if (filter.search() != null) {
            predicates.add("(lower(p.styleCode) like :search escape '!'"
                    + " or exists (select 1 from ProductJpaEntity p2 join p2.translations t"
                    + " where p2 = p and lower(t.name) like :search escape '!')"
                    + " or exists (select 1 from ProductVariantJpaEntity v where v.product = p"
                    + " and (lower(v.sku) like :search escape '!' or v.ean like :search escape '!')))");
            parameters.put("search", CatalogDictionary.containsPattern(filter.search()));
        }
        String where = predicates.isEmpty() ? "" : " where " + String.join(" and ", predicates);

        TypedQuery<Long> countQuery = entityManager.createQuery("select count(p) from ProductJpaEntity p" + where, Long.class);
        TypedQuery<UUID> idQuery = entityManager.createQuery(
                "select p.id from ProductJpaEntity p" + where + " order by p.updatedAt desc, p.id desc", UUID.class);
        parameters.forEach((name, value) -> {
            countQuery.setParameter(name, value);
            idQuery.setParameter(name, value);
        });
        long total = countQuery.getSingleResult();
        List<UUID> ids = idQuery.setFirstResult((int) page.offset()).setMaxResults(page.size()).getResultList();
        if (ids.isEmpty()) {
            return new PageResult<>(List.of(), page.page(), page.size(), total);
        }

        CatalogDictionary dictionary = dictionaryLoader.load();
        List<AdminProductListItem> items = CatalogReadModelAdapter.loadInOrder(entityManager, ids).stream()
                .map(product -> new AdminProductListItem(
                        product.getId(),
                        product.getSlug(),
                        product.getStyleCode(),
                        ProductEntityMapper.translation(product, ADMIN_LANGUAGE).name(),
                        dictionary.categoryName(product.getCategoryId(), ADMIN_LANGUAGE),
                        product.getStatus(),
                        PriceView.of(ProductEntityMapper.pricing(product)),
                        product.getColors().size(),
                        product.getVariants().size(),
                        (int) product.getVariants().stream().filter(ProductVariantJpaEntity::isActive).count(),
                        CatalogDictionary.sortedImages(product).stream().findFirst().map(CatalogDictionary::image).orElse(null),
                        product.getUpdatedAt(),
                        product.getVersion()))
                .toList();
        return new PageResult<>(items, page.page(), page.size(), total);
    }

    @Override
    public Optional<AdminProductView> findProduct(ProductId id) {
        ProductJpaEntity product = entityManager.find(ProductJpaEntity.class, id.value());
        if (product == null) {
            return Optional.empty();
        }
        CatalogDictionary dictionary = dictionaryLoader.load();
        List<UUID> colorOrder = CatalogDictionary.sortedColorIds(product);
        List<ColorSwatchView> colors = colorOrder.stream().map(colorId -> dictionary.swatch(colorId, ADMIN_LANGUAGE)).toList();
        Comparator<ProductVariantJpaEntity> variantOrder = Comparator
                .comparingInt((ProductVariantJpaEntity v) -> colorOrder.indexOf(v.getColorId()))
                .thenComparing(ProductVariantJpaEntity::getSizeId, dictionary.sizeOrder());
        List<AdminProductView.Variant> variants = product.getVariants().stream()
                .sorted(variantOrder)
                .map(v -> new AdminProductView.Variant(v.getId(), v.getColorId(), v.getSizeId(),
                        dictionary.sizeCode(v.getSizeId()), v.getSku(), v.getEan(), v.isActive()))
                .toList();
        return Optional.of(new AdminProductView(
                product.getId(),
                product.getCategoryId(),
                product.getSlug(),
                product.getStyleCode(),
                product.getStatus(),
                PriceView.of(ProductEntityMapper.pricing(product)),
                product.isFeatured(),
                product.getSortOrder(),
                product.getPublishedAt(),
                ProductEntityMapper.translations(product),
                colors,
                variants,
                CatalogDictionary.images(product),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getVersion()));
    }

    @Override
    public List<AdminCategoryView> findCategories() {
        Map<UUID, Long> productCounts = CatalogDictionary.counts(entityManager.createQuery(
                "select p.categoryId, count(p) from ProductJpaEntity p group by p.categoryId", Object[].class).getResultList());
        return dictionaryLoader.load().categoriesSorted().stream()
                .map(category -> new AdminCategoryView(category.getId(), category.getParentId(), category.getSlug(),
                        category.getSortOrder(), category.isVisible(), CategoryPersistenceAdapter.translations(category),
                        productCounts.getOrDefault(category.getId(), 0L), category.getVersion()))
                .toList();
    }

    @Override
    public List<AdminColorView> findColors() {
        Map<UUID, Long> productCounts = CatalogDictionary.counts(entityManager.createQuery(
                "select pc.id.colorId, count(pc) from ProductColorJpaEntity pc group by pc.id.colorId", Object[].class)
                .getResultList());
        return dictionaryLoader.load().colorsSorted().stream()
                .map(color -> new AdminColorView(color.getId(), color.getCode(), color.getHexValue(), color.getSortOrder(),
                        Map.copyOf(color.getNames()), productCounts.getOrDefault(color.getId(), 0L), color.getVersion()))
                .toList();
    }

    @Override
    public List<AdminSizeView> findSizes() {
        Map<UUID, Long> variantCounts = CatalogDictionary.counts(entityManager.createQuery(
                "select v.sizeId, count(v) from ProductVariantJpaEntity v group by v.sizeId", Object[].class).getResultList());
        return dictionaryLoader.load().sizesSorted().stream()
                .map(size -> new AdminSizeView(size.getId(), size.getCode(), size.getSortOrder(),
                        variantCounts.getOrDefault(size.getId(), 0L), size.getVersion()))
                .toList();
    }
}
