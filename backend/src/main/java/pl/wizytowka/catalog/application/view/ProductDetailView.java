package pl.wizytowka.catalog.application.view;

import java.util.List;
import java.util.UUID;

/** Karta produktu w publicznym katalogu (w żądanym języku). */
public record ProductDetailView(UUID id,
                                String slug,
                                String styleCode,
                                String name,
                                String description,
                                String materialComposition,
                                String careInstructions,
                                CategoryRef category,
                                PriceView price,
                                List<ColorOption> colors,
                                List<ImageView> images) {

    public record CategoryRef(UUID id, String slug, String name) {
    }

    /** Kolor z aktywnymi wariantami w kolejności rozmiarówki. */
    public record ColorOption(UUID colorId, String code, String name, String hex, List<VariantOption> variants) {
    }

    public record VariantOption(UUID id, String sizeCode, String sku, String ean) {
    }
}
