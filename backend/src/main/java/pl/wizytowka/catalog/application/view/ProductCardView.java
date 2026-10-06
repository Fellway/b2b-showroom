package pl.wizytowka.catalog.application.view;

import java.util.List;
import java.util.UUID;

/** Kafelek produktu na liście katalogu (w żądanym języku). */
public record ProductCardView(UUID id,
                              String slug,
                              String styleCode,
                              String name,
                              String categorySlug,
                              PriceView price,
                              boolean featured,
                              ImageView cover,
                              List<ColorSwatchView> colors,
                              List<String> sizes) {
}
