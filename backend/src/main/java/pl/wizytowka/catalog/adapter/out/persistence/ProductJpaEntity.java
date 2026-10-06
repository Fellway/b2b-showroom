package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import pl.wizytowka.catalog.domain.ProductStatus;
import pl.wizytowka.shared.adapter.out.persistence.CatalogLanguageConverter;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Korzeń agregatu produktu w modelu persystencji. Kolory, warianty i zdjęcia są stronami odwrotnymi
 * relacji, więc ich zmiana nie podbija wersji automatycznie — robi to {@link #touch()}.
 */
@Entity
@Table(name = "product")
class ProductJpaEntity {

    @Id
    private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "style_code", nullable = false, length = 40)
    private String styleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ProductStatus status;

    @Column(name = "wholesale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal wholesalePrice;

    @Column(name = "retail_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "featured", nullable = false)
    private boolean featured;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "published_at")
    private Instant publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ElementCollection
    @CollectionTable(name = "product_translation", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "locale", length = 2)
    @Convert(converter = CatalogLanguageConverter.class, attributeName = "key")
    private Map<CatalogLanguage, ProductTranslationEmbeddable> translations = new HashMap<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductColorJpaEntity> colors = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariantJpaEntity> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImageJpaEntity> images = new ArrayList<>();

    protected ProductJpaEntity() {
    }

    ProductJpaEntity(UUID id) {
        this.id = id;
    }

    /** Oznacza agregat jako zmieniony: Hibernate podbije wersję i updated_at także przy zmianie samych dzieci. */
    void touch() {
        this.updatedAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    UUID getCategoryId() {
        return categoryId;
    }

    void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    String getSlug() {
        return slug;
    }

    void setSlug(String slug) {
        this.slug = slug;
    }

    String getStyleCode() {
        return styleCode;
    }

    void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    ProductStatus getStatus() {
        return status;
    }

    void setStatus(ProductStatus status) {
        this.status = status;
    }

    BigDecimal getWholesalePrice() {
        return wholesalePrice;
    }

    void setWholesalePrice(BigDecimal wholesalePrice) {
        this.wholesalePrice = wholesalePrice;
    }

    BigDecimal getRetailPrice() {
        return retailPrice;
    }

    void setRetailPrice(BigDecimal retailPrice) {
        this.retailPrice = retailPrice;
    }

    String getCurrency() {
        return currency;
    }

    void setCurrency(String currency) {
        this.currency = currency;
    }

    boolean isFeatured() {
        return featured;
    }

    void setFeatured(boolean featured) {
        this.featured = featured;
    }

    int getSortOrder() {
        return sortOrder;
    }

    void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    Instant getPublishedAt() {
        return publishedAt;
    }

    void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    Long getVersion() {
        return version;
    }

    Map<CatalogLanguage, ProductTranslationEmbeddable> getTranslations() {
        return translations;
    }

    List<ProductColorJpaEntity> getColors() {
        return colors;
    }

    List<ProductVariantJpaEntity> getVariants() {
        return variants;
    }

    List<ProductImageJpaEntity> getImages() {
        return images;
    }
}
