package pl.wizytowka.catalog.application.service;

import pl.wizytowka.catalog.application.port.out.CategoryRepository;
import pl.wizytowka.catalog.application.port.out.ColorRepository;
import pl.wizytowka.catalog.application.port.out.ProductRepository;
import pl.wizytowka.catalog.application.port.out.SizeRepository;
import pl.wizytowka.catalog.domain.Category;
import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.catalog.domain.Color;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.Product;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.Size;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.catalog.domain.Sku;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Proste repozytoria w pamięci do testów serwisów aplikacyjnych (bez wersjonowania). */
final class InMemoryCatalogRepositories {

    final Map<ProductId, Product> products = new HashMap<>();
    final Map<CategoryId, Category> categories = new HashMap<>();
    final Map<ColorId, Color> colors = new HashMap<>();
    final Map<SizeId, Size> sizes = new HashMap<>();

    final ProductRepository productRepository = new ProductRepository() {
        @Override
        public Optional<Product> findById(ProductId id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public void save(Product product) {
            products.put(product.id(), product);
        }

        @Override
        public void delete(Product product) {
            products.remove(product.id());
        }

        @Override
        public boolean existsBySlug(String slug, ProductId excluded) {
            return others(excluded).anyMatch(p -> p.slug().equals(slug));
        }

        @Override
        public boolean existsByStyleCode(String styleCode, ProductId excluded) {
            return others(excluded).anyMatch(p -> p.styleCode().equals(styleCode));
        }

        @Override
        public boolean existsBySkuInOtherProduct(Sku sku, ProductId excluded) {
            return others(excluded).flatMap(p -> p.variants().stream()).anyMatch(v -> v.sku().equals(sku));
        }

        @Override
        public boolean existsByEanInOtherProduct(Gtin ean, ProductId excluded) {
            return others(excluded).flatMap(p -> p.variants().stream()).anyMatch(v -> v.ean().equals(ean));
        }

        @Override
        public boolean existsByCategory(CategoryId categoryId) {
            return products.values().stream().anyMatch(p -> p.categoryId().equals(categoryId));
        }

        @Override
        public boolean existsByColor(ColorId colorId) {
            return products.values().stream().anyMatch(p -> p.colors().contains(colorId));
        }

        @Override
        public boolean existsBySize(SizeId sizeId) {
            return products.values().stream().flatMap(p -> p.variants().stream()).anyMatch(v -> v.sizeId().equals(sizeId));
        }

        private java.util.stream.Stream<Product> others(ProductId excluded) {
            return products.values().stream().filter(p -> !p.id().equals(excluded));
        }
    };

    final CategoryRepository categoryRepository = new CategoryRepository() {
        @Override
        public Optional<Category> findById(CategoryId id) {
            return Optional.ofNullable(categories.get(id));
        }

        @Override
        public void save(Category category) {
            categories.put(category.id(), category);
        }

        @Override
        public void delete(Category category) {
            categories.remove(category.id());
        }

        @Override
        public boolean existsById(CategoryId id) {
            return categories.containsKey(id);
        }

        @Override
        public boolean existsBySlug(String slug, CategoryId excluded) {
            return categories.values().stream()
                    .anyMatch(c -> !c.id().equals(excluded) && c.details().slug().equals(slug));
        }

        @Override
        public boolean hasChildren(CategoryId id) {
            return categories.values().stream().anyMatch(c -> Objects.equals(c.parentId(), id));
        }
    };

    final ColorRepository colorRepository = new ColorRepository() {
        @Override
        public Optional<Color> findById(ColorId id) {
            return Optional.ofNullable(colors.get(id));
        }

        @Override
        public void save(Color color) {
            colors.put(color.id(), color);
        }

        @Override
        public void delete(Color color) {
            colors.remove(color.id());
        }

        @Override
        public boolean existsById(ColorId id) {
            return colors.containsKey(id);
        }

        @Override
        public boolean existsByCode(String code, ColorId excluded) {
            return colors.values().stream().anyMatch(c -> !c.id().equals(excluded) && c.details().code().equals(code));
        }
    };

    final SizeRepository sizeRepository = new SizeRepository() {
        @Override
        public Optional<Size> findById(SizeId id) {
            return Optional.ofNullable(sizes.get(id));
        }

        @Override
        public void save(Size size) {
            sizes.put(size.id(), size);
        }

        @Override
        public void delete(Size size) {
            sizes.remove(size.id());
        }

        @Override
        public boolean existsById(SizeId id) {
            return sizes.containsKey(id);
        }

        @Override
        public boolean existsByCode(String code, SizeId excluded) {
            return sizes.values().stream().anyMatch(s -> !s.id().equals(excluded) && s.code().equals(code));
        }
    };
}
