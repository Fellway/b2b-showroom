package pl.wizytowka.linesheet.domain;

import pl.wizytowka.shared.domain.Guard;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Migawka wariantu z chwili generowania zestawienia. Identyfikatory katalogu są "miękkie":
 * po usunięciu produktu stają się {@code null}, a pozostałe dane zostają bez zmian.
 */
public record LineSheetItem(int position,
                            UUID productId,
                            UUID variantId,
                            String styleCode,
                            String productName,
                            String colorName,
                            String sizeCode,
                            String sku,
                            String ean,
                            BigDecimal wholesalePrice,
                            BigDecimal retailPrice,
                            String imageStorageKey,
                            Integer quantity) {

    public LineSheetItem {
        if (position < 1) {
            throw new InvalidValueException("linesheet.item.position", "Position must be positive");
        }
        styleCode = Guard.requiredText(styleCode, 40, "linesheet.item.style-code");
        productName = Guard.requiredText(productName, 200, "linesheet.item.product-name");
        colorName = Guard.requiredText(colorName, 80, "linesheet.item.color-name");
        sizeCode = Guard.requiredText(sizeCode, 16, "linesheet.item.size-code");
        sku = Guard.requiredText(sku, 64, "linesheet.item.sku");
        ean = Guard.requiredText(ean, 14, "linesheet.item.ean");
        Objects.requireNonNull(wholesalePrice, "wholesalePrice");
        Objects.requireNonNull(retailPrice, "retailPrice");
        imageStorageKey = Guard.optionalText(imageStorageKey, 512, "linesheet.item.image");
        if (quantity != null && quantity <= 0) {
            throw new InvalidValueException("linesheet.item.quantity", "Quantity must be positive");
        }
    }

    /** Wartość hurtowa pozycji lub {@code null}, gdy klient nie podał ilości. */
    public BigDecimal wholesaleTotal() {
        return quantity == null ? null : wholesalePrice.multiply(BigDecimal.valueOf(quantity));
    }
}
