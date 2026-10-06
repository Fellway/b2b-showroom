package pl.wizytowka.catalog.application.port.out;

import pl.wizytowka.catalog.application.view.CatalogFacetsView;
import pl.wizytowka.catalog.application.view.CatalogFilter;
import pl.wizytowka.catalog.application.view.CategoryView;
import pl.wizytowka.catalog.application.view.ProductCardView;
import pl.wizytowka.catalog.application.view.ProductDetailView;
import pl.wizytowka.catalog.application.view.ProductLabel;
import pl.wizytowka.catalog.application.view.VariantSnapshot;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Strona odczytu publicznego katalogu (CQRS-light): zoptymalizowane zapytania zwracające gotowe widoki,
 * z pominięciem ładowania agregatów. Widoczne są wyłącznie produkty PUBLISHED w widocznych kategoriach
 * (kategoria i wszyscy jej przodkowie mają visible = true) oraz wyłącznie aktywne warianty.
 */
public interface CatalogReadModel {

    PageResult<ProductCardView> findPublishedProducts(CatalogFilter filter, CatalogLanguage language, PageQuery page);

    Optional<ProductDetailView> findPublishedProduct(String slug, CatalogLanguage language);

    /** Widoczne kategorie posortowane po sortOrder (płaska lista z parentId — drzewo składa klient). */
    List<CategoryView> findVisibleCategories(CatalogLanguage language);

    CatalogFacetsView findFacets(CatalogLanguage language);

    /** Zwraca tylko warianty możliwe do wyboru (aktywne, w opublikowanym produkcie); pozostałe id pomija. */
    List<VariantSnapshot> findSelectableVariants(Collection<VariantId> variantIds, CatalogLanguage language);

    /** Etykiety produktów w dowolnym statusie; brakujące (usunięte) id są pomijane. */
    Map<ProductId, ProductLabel> findProductLabels(Collection<ProductId> productIds, CatalogLanguage language);
}
