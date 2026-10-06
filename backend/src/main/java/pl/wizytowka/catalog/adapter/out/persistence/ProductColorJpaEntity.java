package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.util.UUID;

/** Wiersz product_color — encja (nie element collection), żeby zmiana kolejności była UPDATE-em, a nie DELETE+INSERT. */
@Entity
@Table(name = "product_color")
class ProductColorJpaEntity {

    @EmbeddedId
    private ProductColorKey id;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private ProductJpaEntity product;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ProductColorJpaEntity() {
    }

    ProductColorJpaEntity(ProductJpaEntity product, UUID colorId, int sortOrder) {
        this.id = new ProductColorKey(product.getId(), colorId);
        this.product = product;
        this.sortOrder = sortOrder;
    }

    UUID getColorId() {
        return id.getColorId();
    }

    int getSortOrder() {
        return sortOrder;
    }

    void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
