package pl.wizytowka.catalog.application.port.in;

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

public interface AdminCatalogQueryUseCase {

    PageResult<AdminProductListItem> products(AdminProductFilter filter, PageQuery page);

    /** @throws pl.wizytowka.shared.domain.ResourceNotFoundException */
    AdminProductView product(ProductId id);

    List<AdminCategoryView> categories();

    List<AdminColorView> colors();

    List<AdminSizeView> sizes();
}
