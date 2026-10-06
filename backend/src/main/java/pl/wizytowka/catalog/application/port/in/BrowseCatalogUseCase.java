package pl.wizytowka.catalog.application.port.in;

import pl.wizytowka.catalog.application.view.CatalogFacetsView;
import pl.wizytowka.catalog.application.view.CatalogFilter;
import pl.wizytowka.catalog.application.view.CategoryView;
import pl.wizytowka.catalog.application.view.ProductCardView;
import pl.wizytowka.catalog.application.view.ProductDetailView;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.List;

/** Publiczny katalog / lookbook. */
public interface BrowseCatalogUseCase {

    PageResult<ProductCardView> products(CatalogFilter filter, CatalogLanguage language, PageQuery page);

    /** @throws pl.wizytowka.shared.domain.ResourceNotFoundException gdy produkt nie istnieje lub nie jest opublikowany */
    ProductDetailView product(String slug, CatalogLanguage language);

    List<CategoryView> categories(CatalogLanguage language);

    CatalogFacetsView facets(CatalogLanguage language);
}
