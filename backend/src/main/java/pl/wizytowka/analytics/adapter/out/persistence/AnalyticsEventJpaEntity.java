package pl.wizytowka.analytics.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.wizytowka.analytics.domain.AnalyticsEvent;
import pl.wizytowka.analytics.domain.AnalyticsEventType;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Wiersz logu zdarzeń (append-only). */
@Entity
@Immutable
@Table(name = "analytics_event")
class AnalyticsEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private AnalyticsEventType type;

    @Column(name = "visitor_id")
    private UUID visitorId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "locale", length = 2)
    private CatalogLanguage language;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "line_sheet_id")
    private UUID lineSheetId;

    @Column(name = "path", length = 512)
    private String path;

    @Column(name = "referrer_host", length = 255)
    private String referrerHost;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", nullable = false)
    private Map<String, String> properties;

    protected AnalyticsEventJpaEntity() {
    }

    static AnalyticsEventJpaEntity of(AnalyticsEvent event) {
        AnalyticsEventJpaEntity entity = new AnalyticsEventJpaEntity();
        entity.occurredAt = event.occurredAt();
        entity.type = event.type();
        entity.visitorId = event.visitorId();
        entity.sessionId = event.sessionId();
        entity.language = event.language();
        entity.productId = event.productId();
        entity.variantId = event.variantId();
        entity.lineSheetId = event.lineSheetId();
        entity.path = event.path();
        entity.referrerHost = event.referrerHost();
        entity.properties = event.properties();
        return entity;
    }

    AnalyticsEvent toDomain() {
        return new AnalyticsEvent(type, occurredAt, visitorId, sessionId, language, productId, variantId, lineSheetId,
                path, referrerHost, properties);
    }
}
