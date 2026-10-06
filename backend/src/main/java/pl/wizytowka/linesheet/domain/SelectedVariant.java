package pl.wizytowka.linesheet.domain;

import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.Pricing;

import java.util.Objects;
import java.util.UUID;

/** Dane wybranego wariantu potrzebne do zbudowania pozycji zestawienia (dostarcza je katalog). */
public record SelectedVariant(UUID productId,
                              UUID variantId,
                              String styleCode,
                              String productName,
                              String colorName,
                              String sizeCode,
                              String sku,
                              String ean,
                              Pricing pricing,
                              String imageStorageKey,
                              Integer quantity) {

    public SelectedVariant {
        Objects.requireNonNull(variantId, "variantId");
        Objects.requireNonNull(pricing, "pricing");
        if (quantity != null && quantity <= 0) {
            throw new InvalidValueException("linesheet.item.quantity", "Quantity must be positive");
        }
    }
}
