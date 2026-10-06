package pl.wizytowka.catalog.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.catalog.application.port.in.ManageProductsUseCase;
import pl.wizytowka.catalog.application.port.in.StatusTransition;
import pl.wizytowka.catalog.application.port.out.CategoryRepository;
import pl.wizytowka.catalog.application.port.out.ColorRepository;
import pl.wizytowka.catalog.application.port.out.ProductRepository;
import pl.wizytowka.catalog.application.port.out.SizeRepository;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.Product;
import pl.wizytowka.catalog.domain.ProductDetails;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.ProductImage;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.catalog.domain.Sku;
import pl.wizytowka.catalog.domain.StoredImage;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.shared.application.Versions;
import pl.wizytowka.shared.application.port.out.FileStorage;
import pl.wizytowka.shared.application.port.out.TransactionalHooks;
import pl.wizytowka.shared.domain.ResourceConflictException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.time.Clock;
import java.util.List;

@Service
@Transactional
class ProductManagementService implements ManageProductsUseCase {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ColorRepository colors;
    private final SizeRepository sizes;
    private final FileStorage fileStorage;
    private final TransactionalHooks transactionalHooks;
    private final Clock clock;

    ProductManagementService(ProductRepository products, CategoryRepository categories, ColorRepository colors,
                             SizeRepository sizes, FileStorage fileStorage, TransactionalHooks transactionalHooks,
                             Clock clock) {
        this.products = products;
        this.categories = categories;
        this.colors = colors;
        this.sizes = sizes;
        this.fileStorage = fileStorage;
        this.transactionalHooks = transactionalHooks;
        this.clock = clock;
    }

    @Override
    public ProductId create(ProductDetails details) {
        validateDetails(details, null);
        Product product = Product.create(details);
        products.save(product);
        return product.id();
    }

    @Override
    public void update(ProductId id, long expectedVersion, ProductDetails details) {
        Product product = load(id);
        Versions.require(expectedVersion, product.version());
        validateDetails(details, id);
        product.updateDetails(details);
        products.save(product);
    }

    @Override
    public void changeStatus(ProductId id, long expectedVersion, StatusTransition transition) {
        Product product = load(id);
        Versions.require(expectedVersion, product.version());
        switch (transition) {
            case PUBLISH -> product.publish(clock.instant());
            case HIDE -> product.hide();
            case ARCHIVE -> product.archive();
            case RESTORE_TO_DRAFT -> product.restoreToDraft();
        }
        products.save(product);
    }

    @Override
    public void delete(ProductId id) {
        Product product = load(id);
        List<StoredImage> files = product.images().stream().map(ProductImage::file).toList();
        products.delete(product);
        deleteFilesAfterCommit(files);
    }

    @Override
    public void addColor(ProductId id, ColorId colorId) {
        Product product = load(id);
        requireColorExists(colorId);
        product.addColor(colorId);
        products.save(product);
    }

    @Override
    public void removeColor(ProductId id, ColorId colorId) {
        Product product = load(id);
        List<StoredImage> removedFiles = product.removeColor(colorId);
        products.save(product);
        deleteFilesAfterCommit(removedFiles);
    }

    @Override
    public void reorderColors(ProductId id, List<ColorId> ordered) {
        Product product = load(id);
        product.reorderColors(ordered);
        products.save(product);
    }

    @Override
    public VariantId addVariant(ProductId id, ColorId colorId, SizeId sizeId, Sku sku, Gtin ean) {
        Product product = load(id);
        requireColorExists(colorId);
        if (!sizes.existsById(sizeId)) {
            throw new ResourceNotFoundException("catalog.size.not-found", "Size not found");
        }
        requireCodesFreeInOtherProducts(id, sku, ean);
        VariantId variantId = product.addVariant(colorId, sizeId, sku, ean);
        products.save(product);
        return variantId;
    }

    @Override
    public void updateVariant(ProductId id, VariantId variantId, Sku sku, Gtin ean, boolean active) {
        Product product = load(id);
        requireCodesFreeInOtherProducts(id, sku, ean);
        product.updateVariant(variantId, sku, ean, active);
        products.save(product);
    }

    @Override
    public void removeVariant(ProductId id, VariantId variantId) {
        Product product = load(id);
        product.removeVariant(variantId);
        products.save(product);
    }

    private Product load(ProductId id) {
        return products.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("catalog.product.not-found", "Product not found"));
    }

    private void validateDetails(ProductDetails details, ProductId self) {
        if (!categories.existsById(details.categoryId())) {
            throw new ResourceNotFoundException("catalog.category.not-found", "Category not found");
        }
        if (products.existsBySlug(details.slug(), self)) {
            throw new ResourceConflictException("catalog.product.slug-taken", "Slug is already used by another product");
        }
        if (products.existsByStyleCode(details.styleCode(), self)) {
            throw new ResourceConflictException("catalog.product.style-code-taken",
                    "Style code is already used by another product");
        }
    }

    private void requireColorExists(ColorId colorId) {
        if (!colors.existsById(colorId)) {
            throw new ResourceNotFoundException("catalog.color.not-found", "Color not found");
        }
    }

    private void requireCodesFreeInOtherProducts(ProductId id, Sku sku, Gtin ean) {
        if (products.existsBySkuInOtherProduct(sku, id)) {
            throw new ResourceConflictException("catalog.variant.sku-taken", "SKU already used: " + sku);
        }
        if (products.existsByEanInOtherProduct(ean, id)) {
            throw new ResourceConflictException("catalog.variant.ean-taken", "EAN already used: " + ean);
        }
    }

    private void deleteFilesAfterCommit(List<StoredImage> files) {
        files.forEach(file -> transactionalHooks.afterCommit(() -> fileStorage.delete(file.storageKey())));
    }
}
