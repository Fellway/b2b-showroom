package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.Pricing;
import pl.wizytowka.shared.domain.Translations;

public final class CatalogTestData {

    public static final String EAN_1 = "5901234123457";
    public static final String EAN_2 = "4006381333931";
    public static final String EAN_3 = "96385074";

    private CatalogTestData() {
    }

    public static ProductDetails details(CategoryId categoryId, String slug, String styleCode) {
        return new ProductDetails(categoryId, slug, styleCode, Pricing.pln("149.00", "349.99"), false, 0,
                Translations.ofDefault(ProductTranslation.named("Koszula lniana")));
    }

    public static Product product() {
        return Product.create(details(CategoryId.newId(), "koszula-lniana", "LS-001"));
    }

    public static StoredImage image(String key) {
        return new StoredImage(key, "image/webp", 1600, 2000, 120_000L);
    }
}
