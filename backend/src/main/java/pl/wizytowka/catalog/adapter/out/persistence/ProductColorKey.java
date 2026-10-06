package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
class ProductColorKey implements Serializable {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "color_id", nullable = false)
    private UUID colorId;

    protected ProductColorKey() {
    }

    ProductColorKey(UUID productId, UUID colorId) {
        this.productId = productId;
        this.colorId = colorId;
    }

    UUID getProductId() {
        return productId;
    }

    UUID getColorId() {
        return colorId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProductColorKey other
                && Objects.equals(productId, other.productId)
                && Objects.equals(colorId, other.colorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, colorId);
    }
}
