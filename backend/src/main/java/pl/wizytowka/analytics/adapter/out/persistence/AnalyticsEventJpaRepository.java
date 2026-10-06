package pl.wizytowka.analytics.adapter.out.persistence;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.wizytowka.analytics.application.port.out.AnalyticsReadModel.ProductCount;
import pl.wizytowka.analytics.domain.AnalyticsEventType;

import java.time.Instant;
import java.util.List;

interface AnalyticsEventJpaRepository extends JpaRepository<AnalyticsEventJpaEntity, Long> {

    @Query("""
            select e.type as type, count(e) as total from AnalyticsEventJpaEntity e
            where e.occurredAt >= :from and e.occurredAt < :to
            group by e.type
            """)
    List<TypeCount> countByType(Instant from, Instant to);

    @Query("""
            select count(distinct e.visitorId) from AnalyticsEventJpaEntity e
            where e.type = pl.wizytowka.analytics.domain.AnalyticsEventType.PAGE_VIEW
              and e.visitorId is not null and e.occurredAt >= :from and e.occurredAt < :to
            """)
    long countUniqueVisitors(Instant from, Instant to);

    @Query("""
            select new pl.wizytowka.analytics.application.port.out.AnalyticsReadModel$ProductCount(e.productId, count(e))
            from AnalyticsEventJpaEntity e
            where e.type = :type and e.productId is not null and e.occurredAt >= :from and e.occurredAt < :to
            group by e.productId
            order by count(e) desc, e.productId asc
            """)
    List<ProductCount> topProducts(AnalyticsEventType type, Instant from, Instant to, Limit limit);

    interface TypeCount {
        AnalyticsEventType getType();

        long getTotal();
    }
}
