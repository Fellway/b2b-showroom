package pl.wizytowka.catalog.domain;

import java.util.Objects;

/** Zdjęcie produktu; {@code colorId == null} oznacza zdjęcie ogólne (lookbook). Kolejność wyznacza pozycja na liście. */
public final class ProductImage {

    private final ImageId id;
    private ColorId colorId;
    private final StoredImage file;

    private ProductImage(ImageId id, ColorId colorId, StoredImage file) {
        this.id = Objects.requireNonNull(id);
        this.colorId = colorId;
        this.file = Objects.requireNonNull(file);
    }

    static ProductImage create(ImageId id, ColorId colorId, StoredImage file) {
        return new ProductImage(id, colorId, file);
    }

    public static ProductImage restore(ImageId id, ColorId colorId, StoredImage file) {
        return new ProductImage(id, colorId, file);
    }

    void assignColor(ColorId colorId) {
        this.colorId = colorId;
    }

    public ImageId id() {
        return id;
    }

    public ColorId colorId() {
        return colorId;
    }

    public StoredImage file() {
        return file;
    }
}
