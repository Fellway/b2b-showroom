package pl.wizytowka.catalog.application.view;

import pl.wizytowka.shared.domain.Guard;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * @param categorySlug kategoria wraz z podkategoriami; {@code null} = wszystkie
 * @param colorIds     produkty mające aktywny wariant w którymkolwiek z kolorów
 * @param sizeIds      produkty mające aktywny wariant w którymkolwiek z rozmiarów
 * @param search       fraza szukana w nazwie (żądany język i bazowy) oraz w kodzie modelu
 */
public record CatalogFilter(String categorySlug, Set<UUID> colorIds, Set<UUID> sizeIds, String search,
                            boolean featuredOnly, CatalogSort sort) {

    public CatalogFilter {
        colorIds = colorIds == null ? Set.of() : Set.copyOf(colorIds);
        sizeIds = sizeIds == null ? Set.of() : Set.copyOf(sizeIds);
        search = Guard.optionalText(search, 100, "catalog.search.too-long");
        sort = Objects.requireNonNullElse(sort, CatalogSort.RECOMMENDED);
    }

    public static CatalogFilter none() {
        return new CatalogFilter(null, Set.of(), Set.of(), null, false, CatalogSort.RECOMMENDED);
    }
}
