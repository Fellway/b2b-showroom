package pl.wizytowka.catalog.application.view;

import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.Sku;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.shared.domain.Pricing;

/**
 * Komplet danych wariantu potrzebny poza katalogiem (np. do zestawienia PDF), w żądanym języku.
 * @param imageStorageKey zdjęcie koloru wariantu lub główne zdjęcie produktu; może być {@code null}
 */
public record VariantSnapshot(VariantId variantId,
                              ProductId productId,
                              String styleCode,
                              String productName,
                              String colorName,
                              String sizeCode,
                              Sku sku,
                              Gtin ean,
                              Pricing pricing,
                              String imageStorageKey) {
}
