package pl.wizytowka.catalog.application.port.out;

import pl.wizytowka.catalog.application.view.AdminCategoryView;
import pl.wizytowka.catalog.application.view.AdminColorView;
import pl.wizytowka.catalog.application.view.AdminProductFilter;
import pl.wizytowka.catalog.application.view.AdminProductListItem;
import pl.wizytowka.catalog.application.view.AdminProductView;
import pl.wizytowka.catalog.application.view.AdminSizeView;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;

import java.util.List;
import java.util.Optional;

/** Strona odczytu panelu administracyjnego: wszystkie statusy, wszystkie języki, nieaktywne warianty. */
public interface AdminCatalogReadModel {

    /** Sortowanie: ostatnio zmienione najpierw. */
    PageResult<AdminProductListItem> findProducts(AdminProductFilter filter, PageQuery page);

    Optional<AdminProductView> findProduct(ProductId id);

    List<AdminCategoryView> findCategories();

    List<AdminColorView> findColors();

    List<AdminSizeView> findSizes();
}
