package pl.wizytowka.catalog.application.port.in;

import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.ProductDetails;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.catalog.domain.Sku;
import pl.wizytowka.catalog.domain.VariantId;

import java.util.List;

/**
 * Zarządzanie produktami w panelu. Operacje przyjmujące {@code expectedVersion} chronią przed
 * nadpisaniem cudzych zmian (wersja pochodzi z {@code AdminProductView#version}).
 */
public interface ManageProductsUseCase {

    ProductId create(ProductDetails details);

    void update(ProductId id, long expectedVersion, ProductDetails details);

    void changeStatus(ProductId id, long expectedVersion, StatusTransition transition);

    /** Trwałe usunięcie wraz z plikami zdjęć. Historyczne zestawienia zachowują swoje migawki. */
    void delete(ProductId id);

    void addColor(ProductId id, ColorId colorId);

    void removeColor(ProductId id, ColorId colorId);

    void reorderColors(ProductId id, List<ColorId> ordered);

    VariantId addVariant(ProductId id, ColorId colorId, SizeId sizeId, Sku sku, Gtin ean);

    void updateVariant(ProductId id, VariantId variantId, Sku sku, Gtin ean, boolean active);

    void removeVariant(ProductId id, VariantId variantId);
}
