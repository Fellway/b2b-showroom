package pl.wizytowka.catalog.adapter.out.persistence;

import pl.wizytowka.catalog.application.port.out.CategoryRepository;
import pl.wizytowka.catalog.application.port.out.ColorRepository;
import pl.wizytowka.catalog.application.port.out.ProductRepository;
import pl.wizytowka.catalog.application.port.out.SizeRepository;
import pl.wizytowka.catalog.domain.Category;
import pl.wizytowka.catalog.domain.CategoryDetails;
import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.catalog.domain.CategoryTranslation;
import pl.wizytowka.catalog.domain.Color;
import pl.wizytowka.catalog.domain.ColorDetails;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.ImageId;
import pl.wizytowka.catalog.domain.Product;
import pl.wizytowka.catalog.domain.ProductDetails;
import pl.wizytowka.catalog.domain.ProductTranslation;
import pl.wizytowka.catalog.domain.Size;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.catalog.domain.Sku;
import pl.wizytowka.catalog.domain.StoredImage;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.Pricing;
import pl.wizytowka.shared.domain.Translations;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/** Buduje dane katalogu wyłącznie przez porty wyjściowe (tak jak robi to warstwa aplikacji). */
final class CatalogFixture {

    private final CategoryRepository categories;
    private final ColorRepository colors;
    private final SizeRepository sizes;
    private final ProductRepository products;
    private int sequence;

    CatalogFixture(CategoryRepository categories, ColorRepository colors, SizeRepository sizes, ProductRepository products) {
        this.categories = categories;
        this.colors = colors;
        this.sizes = sizes;
        this.products = products;
    }

    CategoryId category(String slug, CategoryId parent, boolean visible, String namePl) {
        Category category = Category.create(new CategoryDetails(parent, slug, 0, visible,
                Translations.ofDefault(new CategoryTranslation(namePl, null))));
        categories.save(category);
        return category.id();
    }

    ColorId color(String code, String hex, String namePl, String nameEn) {
        Map<CatalogLanguage, String> names = new EnumMap<>(CatalogLanguage.class);
        names.put(CatalogLanguage.PL, namePl);
        if (nameEn != null) {
            names.put(CatalogLanguage.EN, nameEn);
        }
        Color color = Color.create(new ColorDetails(code, hex, 0, Translations.of(names)));
        colors.save(color);
        return color.id();
    }

    SizeId size(String code, int sortOrder) {
        Size size = Size.create(code, sortOrder);
        sizes.save(size);
        return size.id();
    }

    ProductBuilder product(CategoryId categoryId, String slug, String styleCode) {
        return new ProductBuilder(categoryId, slug, styleCode);
    }

    /** Kolejny poprawny EAN-13 (z cyfrą kontrolną). */
    String nextEan() {
        return ean(++sequence);
    }

    static String ean(int n) {
        String body = "590%09d".formatted(n);
        int sum = 0;
        for (int i = body.length() - 1, weight = 0; i >= 0; i--, weight++) {
            int digit = body.charAt(i) - '0';
            sum += weight % 2 == 0 ? digit * 3 : digit;
        }
        return body + (10 - sum % 10) % 10;
    }

    final class ProductBuilder {

        private final Product product;
        private final Map<CatalogLanguage, ProductTranslation> translations = new EnumMap<>(CatalogLanguage.class);
        private Pricing pricing = Pricing.pln("149.00", "349.99");
        private boolean featured;
        private int sortOrder;
        private final ProductDetails base;

        private ProductBuilder(CategoryId categoryId, String slug, String styleCode) {
            translations.put(CatalogLanguage.PL, ProductTranslation.named("Produkt " + styleCode));
            base = new ProductDetails(categoryId, slug, styleCode, pricing, false, 0, Translations.of(translations));
            product = Product.create(base);
        }

        ProductBuilder name(CatalogLanguage language, String name) {
            translations.put(language, ProductTranslation.named(name));
            return refreshDetails();
        }

        ProductBuilder price(String wholesale, String retail) {
            pricing = Pricing.pln(wholesale, retail);
            return refreshDetails();
        }

        ProductBuilder featured() {
            featured = true;
            return refreshDetails();
        }

        ProductBuilder sortOrder(int value) {
            sortOrder = value;
            return refreshDetails();
        }

        private ProductBuilder refreshDetails() {
            product.updateDetails(new ProductDetails(base.categoryId(), base.slug(), base.styleCode(), pricing, featured,
                    sortOrder, Translations.of(translations)));
            return this;
        }

        ProductBuilder color(ColorId colorId) {
            product.addColor(colorId);
            return this;
        }

        VariantId addVariant(ColorId colorId, SizeId sizeId) {
            return product.addVariant(colorId, sizeId, Sku.of(base.styleCode() + "-" + (++sequence)), Gtin.of(nextEan()));
        }

        ProductBuilder variant(ColorId colorId, SizeId sizeId) {
            addVariant(colorId, sizeId);
            return this;
        }

        ProductBuilder inactiveVariant(ColorId colorId, SizeId sizeId) {
            VariantId id = addVariant(colorId, sizeId);
            var variant = product.variant(id);
            product.updateVariant(id, variant.sku(), variant.ean(), false);
            return this;
        }

        ProductBuilder image(String key, ColorId colorId) {
            product.addImage(ImageId.newId(), new StoredImage(key, "image/webp", 1600, 2000, 100_000L), colorId);
            return this;
        }

        ProductBuilder published(Instant at) {
            product.publish(at);
            return this;
        }

        ProductBuilder hidden() {
            product.hide();
            return this;
        }

        Product build() {
            return product;
        }

        Product save() {
            products.save(product);
            return product;
        }
    }
}
