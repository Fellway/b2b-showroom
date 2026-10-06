package pl.wizytowka.linesheet.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import pl.wizytowka.linesheet.domain.LineSheetItem;
import pl.wizytowka.shared.domain.Uuids;

import java.math.BigDecimal;
import java.util.UUID;

/** Pozycja zestawienia — wiersz niezmienny po zapisie. */
@Entity
@Immutable
@Table(name = "line_sheet_item")
class LineSheetItemJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_sheet_id", nullable = false, updatable = false)
    private LineSheetJpaEntity lineSheet;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "style_code", nullable = false, length = 40)
    private String styleCode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "color_name", nullable = false, length = 80)
    private String colorName;

    @Column(name = "size_code", nullable = false, length = 16)
    private String sizeCode;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "ean", nullable = false, length = 14)
    private String ean;

    @Column(name = "wholesale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal wholesalePrice;

    @Column(name = "retail_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "image_storage_key", length = 512)
    private String imageStorageKey;

    @Column(name = "quantity")
    private Integer quantity;

    protected LineSheetItemJpaEntity() {
    }

    static LineSheetItemJpaEntity of(LineSheetJpaEntity lineSheet, LineSheetItem item) {
        LineSheetItemJpaEntity entity = new LineSheetItemJpaEntity();
        entity.id = Uuids.v7();
        entity.lineSheet = lineSheet;
        entity.position = item.position();
        entity.productId = item.productId();
        entity.variantId = item.variantId();
        entity.styleCode = item.styleCode();
        entity.productName = item.productName();
        entity.colorName = item.colorName();
        entity.sizeCode = item.sizeCode();
        entity.sku = item.sku();
        entity.ean = item.ean();
        entity.wholesalePrice = item.wholesalePrice();
        entity.retailPrice = item.retailPrice();
        entity.imageStorageKey = item.imageStorageKey();
        entity.quantity = item.quantity();
        return entity;
    }

    LineSheetItem toDomain() {
        return new LineSheetItem(position, productId, variantId, styleCode, productName, colorName, sizeCode, sku, ean,
                wholesalePrice, retailPrice, imageStorageKey, quantity);
    }
}
