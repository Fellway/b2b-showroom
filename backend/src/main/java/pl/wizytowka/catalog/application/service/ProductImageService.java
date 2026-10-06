package pl.wizytowka.catalog.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.catalog.application.port.in.ManageProductImagesUseCase;
import pl.wizytowka.catalog.application.port.out.ImageInspector;
import pl.wizytowka.catalog.application.port.out.ImageInspector.InspectedImage;
import pl.wizytowka.catalog.application.port.out.ProductRepository;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.ImageId;
import pl.wizytowka.catalog.domain.Product;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.StoredImage;
import pl.wizytowka.shared.application.port.out.FileStorage;
import pl.wizytowka.shared.application.port.out.TransactionalHooks;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.io.ByteArrayInputStream;
import java.util.List;

@Service
@Transactional
class ProductImageService implements ManageProductImagesUseCase {

    private final ProductRepository products;
    private final ImageInspector imageInspector;
    private final FileStorage fileStorage;
    private final TransactionalHooks transactionalHooks;

    ProductImageService(ProductRepository products, ImageInspector imageInspector, FileStorage fileStorage,
                        TransactionalHooks transactionalHooks) {
        this.products = products;
        this.imageInspector = imageInspector;
        this.fileStorage = fileStorage;
        this.transactionalHooks = transactionalHooks;
    }

    @Override
    public ImageId upload(ProductId productId, ColorId colorId, byte[] content) {
        if (content == null || content.length == 0) {
            throw new InvalidValueException("catalog.image.empty", "Image file is empty");
        }
        if (content.length > MAX_IMAGE_BYTES) {
            throw new InvalidValueException("catalog.image.too-large", "Image exceeds " + MAX_IMAGE_BYTES + " bytes");
        }
        InspectedImage inspected = imageInspector.inspect(content)
                .orElseThrow(() -> new InvalidValueException("catalog.image.unsupported-format",
                        "Supported formats: JPEG, PNG, WebP, AVIF"));
        Product product = load(productId);

        ImageId imageId = ImageId.newId();
        String key = "products/%s/%s.%s".formatted(productId, imageId, inspected.fileExtension());
        StoredImage file = new StoredImage(key, inspected.contentType(), inspected.widthPx(), inspected.heightPx(),
                (long) content.length);
        product.addImage(imageId, file, colorId);

        fileStorage.store(key, new ByteArrayInputStream(content), content.length, inspected.contentType());
        transactionalHooks.afterRollback(() -> fileStorage.delete(key));
        products.save(product);
        return imageId;
    }

    @Override
    public void remove(ProductId productId, ImageId imageId) {
        Product product = load(productId);
        StoredImage removed = product.removeImage(imageId);
        products.save(product);
        transactionalHooks.afterCommit(() -> fileStorage.delete(removed.storageKey()));
    }

    @Override
    public void reorder(ProductId productId, List<ImageId> ordered) {
        Product product = load(productId);
        product.reorderImages(ordered);
        products.save(product);
    }

    @Override
    public void assignColor(ProductId productId, ImageId imageId, ColorId colorId) {
        Product product = load(productId);
        product.assignImageColor(imageId, colorId);
        products.save(product);
    }

    private Product load(ProductId id) {
        return products.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("catalog.product.not-found", "Product not found"));
    }
}
