package pl.wizytowka.linesheet.adapter.out.persistence;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.wizytowka.linesheet.application.view.LineSheetSummaryView;
import pl.wizytowka.linesheet.application.view.ProductLineSheetCount;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LineSheetJpaRepository extends JpaRepository<LineSheetJpaEntity, UUID> {

    Optional<LineSheetJpaEntity> findByPublicId(UUID publicId);

    String SUMMARY = """
            select new pl.wizytowka.linesheet.application.view.LineSheetSummaryView(
                s.id, s.publicId, s.referenceNumber, s.language, s.companyName, s.contactName, s.email, s.phone,
                size(s.items), s.createdAt)
            from LineSheetJpaEntity s
            """;

    @Query(value = SUMMARY + " order by s.createdAt desc, s.id desc",
            countQuery = "select count(s) from LineSheetJpaEntity s")
    Page<LineSheetSummaryView> findSummaries(Pageable pageable);

    @Query("select count(s) from LineSheetJpaEntity s where s.createdAt >= :from and s.createdAt < :to")
    long countCreated(Instant from, Instant to);

    @Query("""
            select new pl.wizytowka.linesheet.application.view.ProductLineSheetCount(i.productId, count(distinct s.id))
            from LineSheetItemJpaEntity i join i.lineSheet s
            where i.productId is not null and s.createdAt >= :from and s.createdAt < :to
            group by i.productId
            order by count(distinct s.id) desc, i.productId asc
            """)
    List<ProductLineSheetCount> topProducts(Instant from, Instant to, Limit limit);

    @Query(value = "select nextval('line_sheet_number_seq')", nativeQuery = true)
    long nextSequenceNumber();
}
