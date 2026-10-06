package pl.wizytowka.catalog.application.port.out;

import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.Product;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.catalog.domain.Sku;

import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(ProductId id);

    /**
     * Zapisuje cały agregat (dane, kolory, warianty, zdjęcia).
     * @throws pl.wizytowka.shared.domain.ConcurrentModificationException gdy wersja w bazie różni się od {@link Product#version()}
     */
    void save(Product product);

    void delete(Product product);

    boolean existsBySlug(String slug, ProductId excluded);

    boolean existsByStyleCode(String styleCode, ProductId excluded);

    /** Czy SKU jest użyte w INNYM produkcie niż {@code excluded} (unikalność w produkcie pilnuje agregat). */
    boolean existsBySkuInOtherProduct(Sku sku, ProductId excluded);

    boolean existsByEanInOtherProduct(Gtin ean, ProductId excluded);

    boolean existsByCategory(CategoryId categoryId);

    boolean existsByColor(ColorId colorId);

    boolean existsBySize(SizeId sizeId);
}
