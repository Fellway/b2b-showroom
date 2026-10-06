package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_image")
class ProductImageJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private ProductJpaEntity product;

    /** {@code null} = zdjęcie ogólne. Spójność z product_color pilnuje złożony FK w bazie. */
    @Column(name = "color_id")
    private UUID colorId;

    @Column(name = "storage_key", nullable = false, length = 512, updatable = false)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100, updatable = false)
    private String contentType;

    @Column(name = "width_px", updatable = false)
    private Integer widthPx;

    @Column(name = "height_px", updatable = false)
    private Integer heightPx;

    @Column(name = "size_bytes", updatable = false)
    private Long sizeBytes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProductImageJpaEntity() {
    }

    ProductImageJpaEntity(UUID id, ProductJpaEntity product, String storageKey, String contentType,
                          Integer widthPx, Integer heightPx, Long sizeBytes) {
        this.id = id;
        this.product = product;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.sizeBytes = sizeBytes;
    }

    UUID getId() {
        return id;
    }

    UUID getColorId() {
        return colorId;
    }

    void setColorId(UUID colorId) {
        this.colorId = colorId;
    }

    String getStorageKey() {
        return storageKey;
    }

    String getContentType() {
        return contentType;
    }

    Integer getWidthPx() {
        return widthPx;
    }

    Integer getHeightPx() {
        return heightPx;
    }

    Long getSizeBytes() {
        return sizeBytes;
    }

    int getSortOrder() {
        return sortOrder;
    }

    void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
