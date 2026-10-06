package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_variant")
class ProductVariantJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private ProductJpaEntity product;

    @Column(name = "color_id", nullable = false, updatable = false)
    private UUID colorId;

    @Column(name = "size_id", nullable = false, updatable = false)
    private UUID sizeId;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "ean", nullable = false, length = 14)
    private String ean;

    @Column(name = "active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductVariantJpaEntity() {
    }

    ProductVariantJpaEntity(UUID id, ProductJpaEntity product, UUID colorId, UUID sizeId) {
        this.id = id;
        this.product = product;
        this.colorId = colorId;
        this.sizeId = sizeId;
    }

    void update(String sku, String ean, boolean active) {
        this.sku = sku;
        this.ean = ean;
        this.active = active;
    }

    UUID getId() {
        return id;
    }

    ProductJpaEntity getProduct() {
        return product;
    }

    UUID getColorId() {
        return colorId;
    }

    UUID getSizeId() {
        return sizeId;
    }

    String getSku() {
        return sku;
    }

    String getEan() {
        return ean;
    }

    boolean isActive() {
        return active;
    }
}
