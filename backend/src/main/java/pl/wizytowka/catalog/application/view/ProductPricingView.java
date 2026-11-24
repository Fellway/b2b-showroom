package pl.wizytowka.catalog.application.view;

import java.util.List;
import java.util.UUID;

/**
 * Dane handlowe produktu widoczne wyłącznie dla zalogowanego kupca (D23): ceny i kody EAN aktywnych wariantów.
 * Publiczny katalog ({@link ProductCardView}, {@link ProductDetailView}) ich nie udostępnia.
 */
public record ProductPricingView(UUID productId, PriceView price, List<VariantCode> variants) {

    public ProductPricingView {
        variants = List.copyOf(variants);
    }

    public record VariantCode(UUID variantId, String sku, String ean) {
    }
}
