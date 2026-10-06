package pl.wizytowka.catalog.domain;

import java.util.Objects;

/** Pozycja handlowa: kolor x rozmiar, identyfikowana przez SKU i EAN. Część agregatu {@link Product}. */
public final class ProductVariant {

    private final VariantId id;
    private final ColorId colorId;
    private final SizeId sizeId;
    private Sku sku;
    private Gtin ean;
    private boolean active;

    private ProductVariant(VariantId id, ColorId colorId, SizeId sizeId, Sku sku, Gtin ean, boolean active) {
        this.id = Objects.requireNonNull(id);
        this.colorId = Objects.requireNonNull(colorId);
        this.sizeId = Objects.requireNonNull(sizeId);
        this.sku = Objects.requireNonNull(sku);
        this.ean = Objects.requireNonNull(ean);
        this.active = active;
    }

    static ProductVariant create(ColorId colorId, SizeId sizeId, Sku sku, Gtin ean) {
        return new ProductVariant(VariantId.newId(), colorId, sizeId, sku, ean, true);
    }

    public static ProductVariant restore(VariantId id, ColorId colorId, SizeId sizeId, Sku sku, Gtin ean, boolean active) {
        return new ProductVariant(id, colorId, sizeId, sku, ean, active);
    }

    void update(Sku sku, Gtin ean, boolean active) {
        this.sku = Objects.requireNonNull(sku);
        this.ean = Objects.requireNonNull(ean);
        this.active = active;
    }

    public VariantId id() {
        return id;
    }

    public ColorId colorId() {
        return colorId;
    }

    public SizeId sizeId() {
        return sizeId;
    }

    public Sku sku() {
        return sku;
    }

    public Gtin ean() {
        return ean;
    }

    public boolean isActive() {
        return active;
    }
}
