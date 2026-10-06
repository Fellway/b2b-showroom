package pl.wizytowka.catalog.adapter.out.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.catalog.application.port.out.ProductRepository;
import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.Product;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.catalog.domain.Sku;
import pl.wizytowka.shared.domain.ConcurrentModificationException;

import java.util.Objects;
import java.util.Optional;

@Repository
@Transactional
class ProductPersistenceAdapter implements ProductRepository {

    private final ProductJpaRepository products;

    ProductPersistenceAdapter(ProductJpaRepository products) {
        this.products = products;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return products.findById(id.value()).map(ProductEntityMapper::toDomain);
    }

    @Override
    public void save(Product product) {
        Optional<ProductJpaEntity> existing = products.findById(product.id().value());
        if (existing.isEmpty()) {
            ProductJpaEntity entity = new ProductJpaEntity(product.id().value());
            ProductEntityMapper.apply(product, entity);
            products.save(entity);
            return;
        }
        ProductJpaEntity entity = existing.get();
        if (!Objects.equals(entity.getVersion(), product.version())) {
            throw new ConcurrentModificationException("catalog.concurrent-modification",
                    "Product was modified by someone else; reload and try again");
        }
        ProductEntityMapper.apply(product, entity);
        // kolory/warianty/zdjęcia to strony odwrotne relacji — jawne oznaczenie zmiany podbija wersję agregatu
        entity.touch();
    }

    @Override
    public void delete(Product product) {
        products.findById(product.id().value()).ifPresent(products::delete);
    }

    @Override
    public boolean existsBySlug(String slug, ProductId excluded) {
        return excluded == null ? products.existsBySlug(slug) : products.existsBySlugAndIdNot(slug, excluded.value());
    }

    @Override
    public boolean existsByStyleCode(String styleCode, ProductId excluded) {
        return excluded == null
                ? products.existsByStyleCode(styleCode)
                : products.existsByStyleCodeAndIdNot(styleCode, excluded.value());
    }

    @Override
    public boolean existsBySkuInOtherProduct(Sku sku, ProductId excluded) {
        return excluded == null
                ? products.existsVariantBySku(sku.value())
                : products.existsVariantBySkuOutside(sku.value(), excluded.value());
    }

    @Override
    public boolean existsByEanInOtherProduct(Gtin ean, ProductId excluded) {
        return excluded == null
                ? products.existsVariantByEan(ean.value())
                : products.existsVariantByEanOutside(ean.value(), excluded.value());
    }

    @Override
    public boolean existsByCategory(CategoryId categoryId) {
        return products.existsByCategoryId(categoryId.value());
    }

    @Override
    public boolean existsByColor(ColorId colorId) {
        return products.existsColorUsage(colorId.value());
    }

    @Override
    public boolean existsBySize(SizeId sizeId) {
        return products.existsSizeUsage(sizeId.value());
    }
}
