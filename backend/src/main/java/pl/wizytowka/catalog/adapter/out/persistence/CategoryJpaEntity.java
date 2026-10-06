package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import pl.wizytowka.shared.adapter.out.persistence.CatalogLanguageConverter;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "category")
class CategoryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "visible", nullable = false)
    private boolean visible;

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
    @CollectionTable(name = "category_translation", joinColumns = @JoinColumn(name = "category_id"))
    @MapKeyColumn(name = "locale", length = 2)
    @Convert(converter = CatalogLanguageConverter.class, attributeName = "key")
    private Map<CatalogLanguage, CategoryTranslationEmbeddable> translations = new HashMap<>();

    protected CategoryJpaEntity() {
    }

    CategoryJpaEntity(UUID id) {
        this.id = id;
    }

    void update(UUID parentId, String slug, int sortOrder, boolean visible) {
        this.parentId = parentId;
        this.slug = slug;
        this.sortOrder = sortOrder;
        this.visible = visible;
    }

    UUID getId() {
        return id;
    }

    UUID getParentId() {
        return parentId;
    }

    String getSlug() {
        return slug;
    }

    int getSortOrder() {
        return sortOrder;
    }

    boolean isVisible() {
        return visible;
    }

    Long getVersion() {
        return version;
    }

    Map<CatalogLanguage, CategoryTranslationEmbeddable> getTranslations() {
        return translations;
    }
}
