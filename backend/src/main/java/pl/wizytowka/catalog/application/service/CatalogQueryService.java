package pl.wizytowka.catalog.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.catalog.application.port.in.AdminCatalogQueryUseCase;
import pl.wizytowka.catalog.application.port.in.BrowseCatalogUseCase;
import pl.wizytowka.catalog.application.port.in.BuyerPricingQuery;
import pl.wizytowka.catalog.application.port.in.ProductLabelQuery;
import pl.wizytowka.catalog.application.port.in.VariantSnapshotQuery;
import pl.wizytowka.catalog.application.port.out.AdminCatalogReadModel;
import pl.wizytowka.catalog.application.port.out.CatalogReadModel;
import pl.wizytowka.catalog.application.view.AdminCategoryView;
import pl.wizytowka.catalog.application.view.AdminColorView;
import pl.wizytowka.catalog.application.view.AdminProductFilter;
import pl.wizytowka.catalog.application.view.AdminProductListItem;
import pl.wizytowka.catalog.application.view.AdminProductView;
import pl.wizytowka.catalog.application.view.AdminSizeView;
import pl.wizytowka.catalog.application.view.CatalogFacetsView;
import pl.wizytowka.catalog.application.view.CatalogFilter;
import pl.wizytowka.catalog.application.view.CategoryView;
import pl.wizytowka.catalog.application.view.ProductCardView;
import pl.wizytowka.catalog.application.view.ProductDetailView;
import pl.wizytowka.catalog.application.view.ProductLabel;
import pl.wizytowka.catalog.application.view.ProductPricingView;
import pl.wizytowka.catalog.application.view.VariantSnapshot;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
class CatalogQueryService implements BrowseCatalogUseCase, AdminCatalogQueryUseCase, VariantSnapshotQuery, ProductLabelQuery,
        BuyerPricingQuery {

    private final CatalogReadModel catalog;
    private final AdminCatalogReadModel adminCatalog;

    CatalogQueryService(CatalogReadModel catalog, AdminCatalogReadModel adminCatalog) {
        this.catalog = catalog;
        this.adminCatalog = adminCatalog;
    }

    @Override
    public PageResult<ProductCardView> products(CatalogFilter filter, CatalogLanguage language, PageQuery page) {
        return catalog.findPublishedProducts(filter, language, page);
    }

    @Override
    public ProductDetailView product(String slug, CatalogLanguage language) {
        return catalog.findPublishedProduct(slug, language)
                .orElseThrow(() -> new ResourceNotFoundException("catalog.product.not-found", "Product not found"));
    }

    @Override
    public List<CategoryView> categories(CatalogLanguage language) {
        return catalog.findVisibleCategories(language);
    }

    @Override
    public CatalogFacetsView facets(CatalogLanguage language) {
        return catalog.findFacets(language);
    }

    @Override
    public List<VariantSnapshot> selectableVariants(Collection<VariantId> variantIds, CatalogLanguage language) {
        Set<VariantId> requested = new LinkedHashSet<>(variantIds);
        if (requested.size() != variantIds.size()) {
            throw new InvalidValueException("catalog.variant.duplicate", "Each variant may be selected only once");
        }
        List<VariantSnapshot> snapshots = catalog.findSelectableVariants(requested, language);
        if (snapshots.size() != requested.size()) {
            throw new BusinessRuleViolationException("catalog.variant.not-selectable",
                    "Some selected variants are no longer available");
        }
        return snapshots;
    }

    @Override
    public List<ProductPricingView> pricing(Collection<ProductId> productIds) {
        Set<ProductId> requested = new LinkedHashSet<>(productIds);
        if (requested.size() > MAX_PRODUCTS) {
            throw new InvalidValueException("catalog.pricing.too-many",
                    "At most " + MAX_PRODUCTS + " products can be requested at once");
        }
        return requested.isEmpty() ? List.of() : catalog.findPublishedPricing(requested);
    }

    @Override
    public Map<ProductId, ProductLabel> labels(Collection<ProductId> productIds, CatalogLanguage language) {
        return productIds.isEmpty() ? Map.of() : catalog.findProductLabels(productIds, language);
    }

    @Override
    public PageResult<AdminProductListItem> products(AdminProductFilter filter, PageQuery page) {
        return adminCatalog.findProducts(filter, page);
    }

    @Override
    public AdminProductView product(ProductId id) {
        return adminCatalog.findProduct(id)
                .orElseThrow(() -> new ResourceNotFoundException("catalog.product.not-found", "Product not found"));
    }

    @Override
    public List<AdminCategoryView> categories() {
        return adminCatalog.findCategories();
    }

    @Override
    public List<AdminColorView> colors() {
        return adminCatalog.findColors();
    }

    @Override
    public List<AdminSizeView> sizes() {
        return adminCatalog.findSizes();
    }
}
