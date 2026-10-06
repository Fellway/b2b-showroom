package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "size")
class SizeJpaEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, length = 16)
    private String code;

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

    protected SizeJpaEntity() {
    }

    SizeJpaEntity(UUID id) {
        this.id = id;
    }

    void update(String code, int sortOrder) {
        this.code = code;
        this.sortOrder = sortOrder;
    }

    UUID getId() {
        return id;
    }

    String getCode() {
        return code;
    }

    int getSortOrder() {
        return sortOrder;
    }

    Long getVersion() {
        return version;
    }
}
