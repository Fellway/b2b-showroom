package pl.wizytowka.linesheet.adapter.out.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.wizytowka.linesheet.application.port.out.LineSheetNumberGenerator;
import pl.wizytowka.linesheet.application.port.out.LineSheetReadModel;
import pl.wizytowka.linesheet.application.port.out.LineSheetRepository;
import pl.wizytowka.linesheet.application.view.LineSheetSummaryView;
import pl.wizytowka.linesheet.application.view.ProductLineSheetCount;
import pl.wizytowka.linesheet.domain.LineSheet;
import pl.wizytowka.linesheet.domain.LineSheetItem;
import pl.wizytowka.linesheet.domain.Recipient;
import pl.wizytowka.linesheet.domain.SelectedVariant;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.shared.domain.Pricing;
import pl.wizytowka.support.PersistenceTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PersistenceTest
class LineSheetPersistenceAdapterTest {

    private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");

    @Autowired
    LineSheetRepository repository;

    @Autowired
    LineSheetReadModel readModel;

    @Autowired
    LineSheetNumberGenerator numbers;

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    UUID productA;
    UUID variantA;
    UUID productB;
    UUID variantB;

    /** Minimalny katalog w SQL — moduł zestawień nie zależy od adapterów katalogu. */
    @BeforeEach
    void insertCatalogRows() {
        UUID category = UUID.randomUUID();
        UUID color = UUID.randomUUID();
        UUID size = UUID.randomUUID();
        jdbc.update("insert into category (id, slug, created_at, updated_at) values (?, 'koszule', now(), now())", category);
        jdbc.update("insert into color (id, code, created_at, updated_at) values (?, 'NAVY', now(), now())", color);
        jdbc.update("insert into size (id, code, created_at, updated_at) values (?, 'M', now(), now())", size);
        productA = UUID.randomUUID();
        variantA = UUID.randomUUID();
        productB = UUID.randomUUID();
        variantB = UUID.randomUUID();
        insertProduct(productA, variantA, category, color, size, "A", "5901234123457");
        insertProduct(productB, variantB, category, color, size, "B", "4006381333931");
    }

    private void insertProduct(UUID product, UUID variant, UUID category, UUID color, UUID size, String code, String ean) {
        jdbc.update("""
                insert into product (id, category_id, slug, style_code, wholesale_price, retail_price, created_at, updated_at)
                values (?, ?, ?, ?, 100, 200, now(), now())""", product, category, "p-" + code.toLowerCase(), code);
        jdbc.update("insert into product_color (product_id, color_id) values (?, ?)", product, color);
        jdbc.update("""
                insert into product_variant (id, product_id, color_id, size_id, sku, ean, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, now(), now())""", variant, product, color, size, code + "-NAV-M", ean);
    }

    @Test
    void persistsSnapshotAndUpdatesOnlyTechnicalState() {
        LineSheet sheet = sheet(T0, true, item(productA, variantA, "A-NAV-M", 12), item(productB, variantB, "B-NAV-M", null));
        repository.save(sheet);
        em.flush();
        em.clear();

        LineSheet loaded = repository.findByPublicId(sheet.publicId()).orElseThrow();
        assertThat(loaded.referenceNumber()).isEqualTo(sheet.referenceNumber());
        assertThat(loaded.language()).isEqualTo(CatalogLanguage.EN);
        assertThat(loaded.recipient()).isEqualTo(sheet.recipient());
        assertThat(loaded.items()).extracting(LineSheetItem::position, LineSheetItem::sku, LineSheetItem::quantity)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1, "A-NAV-M", 12),
                        org.assertj.core.groups.Tuple.tuple(2, "B-NAV-M", null));
        assertThat(loaded.items().getFirst().wholesalePrice()).isEqualByComparingTo("149.00");

        loaded.attachPdf("line-sheets/2026/" + loaded.publicId() + ".pdf", T0);
        repository.save(loaded);
        em.flush();
        em.clear();

        LineSheet withPdf = repository.findById(sheet.id()).orElseThrow();
        assertThat(withPdf.pdfStorageKey()).isEqualTo("line-sheets/2026/" + sheet.publicId() + ".pdf");
        assertThat(withPdf.pdfGeneratedAt()).isEqualTo(T0);
        assertThat(withPdf.version()).isEqualTo(1);
        assertThatThrownBy(() -> repository.save(loaded)).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void productDeletionKeepsSnapshot() {
        LineSheet sheet = sheet(T0, false, item(productA, variantA, "A-NAV-M", null));
        repository.save(sheet);
        em.flush();

        jdbc.update("delete from product where id = ?", productA);
        em.clear();

        LineSheetItem item = repository.findById(sheet.id()).orElseThrow().items().getFirst();
        assertThat(item.productId()).isNull();
        assertThat(item.variantId()).isNull();
        assertThat(item.productName()).isEqualTo("Linen shirt");
    }

    @Test
    void readModelPagesAndRanksProducts() {
        LineSheet first = sheet(T0, true, item(productA, variantA, "A-NAV-M", null), item(productB, variantB, "B-NAV-M", null));
        first.attachPdf("line-sheets/2026/a.pdf", T0);
        repository.save(first);
        repository.save(sheet(T0.plusSeconds(60), false, item(productA, variantA, "A-NAV-M", null)));
        repository.save(sheet(T0.plusSeconds(7200), false, item(productB, variantB, "B-NAV-M", null)));
        em.flush();
        em.clear();

        PageResult<LineSheetSummaryView> page = readModel.findSummaries(new PageQuery(0, 2));
        assertThat(page.totalItems()).isEqualTo(3);
        assertThat(page.items()).extracting(LineSheetSummaryView::createdAt)
                .containsExactly(T0.plusSeconds(7200), T0.plusSeconds(60));
        assertThat(readModel.findSummaries(new PageQuery(1, 2)).items())
                .singleElement().satisfies(s -> {
                    assertThat(s.itemCount()).isEqualTo(2);
                    assertThat(s.email()).isEqualTo("buyer@boutique.cz");
                });

        Instant to = T0.plusSeconds(3600);
        assertThat(readModel.countCreated(T0, to)).isEqualTo(2);
        assertThat(readModel.topProducts(T0, to, 10)).containsExactly(
                new ProductLineSheetCount(productA, 2), new ProductLineSheetCount(productB, 1));
    }

    @Test
    void generatesIncreasingNumbers() {
        long first = numbers.nextSequenceNumber();

        assertThat(numbers.nextSequenceNumber()).isGreaterThan(first);
    }

    private static int counter;

    private static LineSheet sheet(Instant createdAt, boolean withContact, SelectedVariant... items) {
        Recipient recipient = withContact
                ? new Recipient("Boutique s.r.o.", "Jana", "buyer@boutique.cz", null, "CZ12345678")
                : Recipient.anonymous();
        return LineSheet.create(LineSheet.formatReference(2026, ++counter), CatalogLanguage.EN, recipient, null,
                withContact, null, UUID.randomUUID(), null, List.of(items), createdAt);
    }

    private static SelectedVariant item(UUID product, UUID variant, String sku, Integer quantity) {
        return new SelectedVariant(product, variant, "LS-100", "Linen shirt", "Navy", "M", sku, "5901234123457",
                Pricing.pln("149.00", "349.99"), null, quantity);
    }
}
