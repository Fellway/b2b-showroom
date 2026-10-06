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
@Table(name = "color")
class ColorJpaEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "hex_value", length = 7)
    private String hexValue;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

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
    @CollectionTable(name = "color_translation", joinColumns = @JoinColumn(name = "color_id"))
    @MapKeyColumn(name = "locale", length = 2)
    @Convert(converter = CatalogLanguageConverter.class, attributeName = "key")
    @Column(name = "name", nullable = false, length = 80)
    private Map<CatalogLanguage, String> names = new HashMap<>();

    protected ColorJpaEntity() {
    }

    ColorJpaEntity(UUID id) {
        this.id = id;
    }

    void update(String code, String hexValue, int sortOrder) {
        this.code = code;
        this.hexValue = hexValue;
        this.sortOrder = sortOrder;
    }

    UUID getId() {
        return id;
    }

    String getCode() {
        return code;
    }

    String getHexValue() {
        return hexValue;
    }

    int getSortOrder() {
        return sortOrder;
    }

    Long getVersion() {
        return version;
    }

    Map<CatalogLanguage, String> getNames() {
        return names;
    }
}
