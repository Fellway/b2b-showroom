package pl.wizytowka.catalog.application.view;

import pl.wizytowka.catalog.domain.ProductStatus;
import pl.wizytowka.shared.domain.Guard;

import java.util.UUID;

/** Wszystkie pola opcjonalne. {@code search} przeszukuje nazwy (wszystkie języki), kod modelu, SKU i EAN. */
public record AdminProductFilter(String search, ProductStatus status, UUID categoryId) {

    public AdminProductFilter {
        search = Guard.optionalText(search, 100, "catalog.search.too-long");
    }
}
