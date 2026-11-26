package pl.wizytowka.linesheet.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.wizytowka.analytics.application.port.in.ClientEvent;
import pl.wizytowka.analytics.application.port.in.RecordAnalyticsEventsUseCase;
import pl.wizytowka.analytics.application.port.in.ServerEvent;
import pl.wizytowka.analytics.domain.AnalyticsEventType;
import pl.wizytowka.catalog.application.port.in.VariantSnapshotQuery;
import pl.wizytowka.catalog.application.view.VariantSnapshot;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.Sku;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.contact.application.port.in.SalesRepresentativeQuery;
import pl.wizytowka.contact.application.view.AdminSalesRepresentativeView;
import pl.wizytowka.contact.application.view.SalesRepresentativeContact;
import pl.wizytowka.contact.domain.SalesRepresentativeId;
import pl.wizytowka.identity.application.port.in.BuyerContactQuery.BuyerContact;
import pl.wizytowka.linesheet.application.port.in.CreateLineSheetCommand;
import pl.wizytowka.linesheet.application.port.out.LineSheetDocumentRenderer;
import pl.wizytowka.linesheet.application.port.out.LineSheetRepository;
import pl.wizytowka.linesheet.application.view.CreatedLineSheet;
import pl.wizytowka.linesheet.application.view.LineSheetDocument;
import pl.wizytowka.linesheet.application.view.LineSheetPdf;
import pl.wizytowka.linesheet.domain.LineSheet;
import pl.wizytowka.linesheet.domain.LineSheetId;
import pl.wizytowka.shared.application.port.out.FileStorage;
import pl.wizytowka.shared.application.port.out.TransactionalHooks;
import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.Pricing;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LineSheetServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");
    private static final byte[] PDF = "%PDF-1.7 test".getBytes();
    private static final UUID BUYER = UUID.fromString("01900000-0000-7000-8000-0000000000b1");

    private final Map<VariantId, VariantSnapshot> catalog = new HashMap<>();
    private final InMemoryLineSheets lineSheets = new InMemoryLineSheets();
    private final InMemoryStorage storage = new InMemoryStorage();
    private final DeferredHooks hooks = new DeferredHooks();
    private final RecordingAnalytics analytics = new RecordingAnalytics();
    private final List<SalesRepresentativeContact> representatives = new ArrayList<>();
    private final List<LineSheetDocument> rendered = new ArrayList<>();
    /** Aktywni kupcy (BuyerContactQuery zwraca tylko aktywne konta). */
    private final Map<UUID, BuyerContact> activeBuyers = new HashMap<>();
    private LineSheetService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        LineSheetDocumentRenderer renderer = document -> {
            rendered.add(document);
            return PDF;
        };
        long[] sequence = {41};
        activeBuyers.put(BUYER, new BuyerContact(BUYER, "Boutique s.r.o.", "Jana", "buyer@boutique.cz",
                "+420 777 100 200", "CZ 12345678", CatalogLanguage.EN));
        service = new LineSheetService(lineSheets, () -> ++sequence[0], renderer, new StubCatalog(),
                new StubRepresentatives(), buyerId -> Optional.ofNullable(activeBuyers.get(buyerId)), analytics,
                storage, hooks, clock);
    }

    @Test
    void createsLineSheetStoresPdfAndRecordsAnalyticsAfterCommit() {
        VariantSnapshot variant = variant("LS100-NAV-M");
        SalesRepresentativeContact rep = representative();

        CreatedLineSheet created = service.create(command(List.of(variant.variantId()), null));

        assertThat(created.referenceNumber()).isEqualTo("LS-2026-000042");
        LineSheet saved = lineSheets.findByPublicId(created.publicId()).orElseThrow();
        assertThat(saved.pdfStorageKey()).isEqualTo("line-sheets/2026/" + created.publicId() + ".pdf");
        assertThat(storage.files).containsKey(saved.pdfStorageKey());
        assertThat(saved.salesRepresentativeId()).as("domyślnie pierwszy aktywny opiekun").isEqualTo(rep.id());
        assertThat(saved.items()).singleElement().satisfies(item -> {
            assertThat(item.sku()).isEqualTo("LS100-NAV-M");
            assertThat(item.quantity()).isEqualTo(6);
        });
        assertThat(analytics.serverEvents).as("analityka dopiero po commicie").isEmpty();

        hooks.commit();

        assertThat(analytics.serverEvents).singleElement().satisfies(e -> {
            assertThat(e.type()).isEqualTo(AnalyticsEventType.LINE_SHEET_GENERATED);
            assertThat(e.lineSheetId()).isEqualTo(saved.id().value());
        });
    }

    @Test
    void rendersPdfFromSnapshotWithBuyerContactAndSalesRepresentative() {
        VariantSnapshot variant = variant("LS100-NAV-M");
        SalesRepresentativeContact rep = representative();

        CreatedLineSheet created = service.create(command(List.of(variant.variantId()), null));

        assertThat(rendered).singleElement().satisfies(document -> {
            assertThat(document.publicId()).isEqualTo(created.publicId());
            assertThat(document.language()).isEqualTo(CatalogLanguage.EN);
            assertThat(document.recipient().email()).as("dane kontaktowe to migawka konta kupca")
                    .isEqualTo("buyer@boutique.cz");
            assertThat(document.recipient().companyName()).isEqualTo("Boutique s.r.o.");
            assertThat(document.recipient().taxId()).isEqualTo("CZ 12345678");
            assertThat(document.salesRepresentative()).isEqualTo(rep);
            assertThat(document.items()).extracting(item -> item.sku()).containsExactly("LS100-NAV-M");
        });
    }

    @Test
    void removesStoredPdfWhenTransactionRollsBack() {
        VariantSnapshot variant = variant("LS100-NAV-M");
        service.create(command(List.of(variant.variantId()), null));

        hooks.rollback();

        assertThat(storage.files).isEmpty();
    }

    @Test
    void rejectsDuplicatesUnknownRepresentativeAndUnavailableVariants() {
        VariantSnapshot variant = variant("LS100-NAV-M");
        assertThatThrownBy(() -> service.create(command(List.of(variant.variantId(), variant.variantId()), null)))
                .hasFieldOrPropertyWithValue("code", "linesheet.items.duplicate");
        assertThatThrownBy(() -> service.create(command(List.of(variant.variantId()), UUID.randomUUID())))
                .isInstanceOf(InvalidValueException.class)
                .hasFieldOrPropertyWithValue("code", "linesheet.sales-representative.unknown");
        assertThatThrownBy(() -> service.create(command(List.of(VariantId.newId()), null)))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(lineSheets.store).isEmpty();
    }

    @Test
    void onlyActiveBuyerCanGenerateLineSheet() {
        VariantSnapshot variant = variant("LS100-NAV-M");
        activeBuyers.clear();

        assertThatThrownBy(() -> service.create(command(List.of(variant.variantId()), null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("code", "linesheet.buyer.not-active");
        assertThat(lineSheets.store).isEmpty();
        assertThat(storage.files).as("PDF nie jest renderowany dla nieaktywnego konta").isEmpty();
    }

    @Test
    void servesPdfOnlyToOwnerAndStaff() throws IOException {
        VariantSnapshot variant = variant("LS100-NAV-M");
        CreatedLineSheet created = service.create(command(List.of(variant.variantId()), null));
        LineSheet saved = lineSheets.findByPublicId(created.publicId()).orElseThrow();
        assertThat(saved.buyerAccountId()).isEqualTo(BUYER);

        try (LineSheetPdf pdf = service.pdfForBuyer(BUYER, created.publicId())) {
            assertThat(pdf.content().readAllBytes()).isEqualTo(PDF);
            assertThat(pdf.sizeBytes()).isEqualTo(PDF.length);
            assertThat(pdf.fileName()).isEqualTo("LS-2026-000042.pdf");
        }
        try (LineSheetPdf pdf = service.pdfForStaff(saved.id())) {
            assertThat(pdf.content().readAllBytes()).isEqualTo(PDF);
        }
        assertThatThrownBy(() -> service.pdfForBuyer(UUID.randomUUID(), created.publicId()))
                .as("cudze zestawienie jest nieodróżnialne od nieistniejącego")
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.pdfForBuyer(BUYER, UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------ fixtures & fakes

    private VariantSnapshot variant(String sku) {
        VariantSnapshot snapshot = new VariantSnapshot(VariantId.newId(), ProductId.newId(), "LS-100", "Linen shirt",
                "Navy", "M", Sku.of(sku), Gtin.of("5901234123457"), Pricing.pln("149.00", "349.99"), null);
        catalog.put(snapshot.variantId(), snapshot);
        return snapshot;
    }

    private SalesRepresentativeContact representative() {
        SalesRepresentativeContact contact = new SalesRepresentativeContact(UUID.randomUUID(), "Anna Nowak",
                "anna@brand.pl", "+48 600 100 200");
        representatives.add(contact);
        return contact;
    }

    private static CreateLineSheetCommand command(List<VariantId> variants, UUID representativeId) {
        return new CreateLineSheetCommand(BUYER, CatalogLanguage.EN,
                variants.stream().map(v -> new CreateLineSheetCommand.Selection(v.value(), 6)).toList(),
                null, representativeId, UUID.randomUUID());
    }

    private final class StubCatalog implements VariantSnapshotQuery {
        @Override
        public List<VariantSnapshot> selectableVariants(Collection<VariantId> variantIds, CatalogLanguage language) {
            return variantIds.stream().map(id -> Optional.ofNullable(catalog.get(id)).orElseThrow(() ->
                    new BusinessRuleViolationException("catalog.variant.not-selectable", "Variant not selectable"))).toList();
        }
    }

    private final class StubRepresentatives implements SalesRepresentativeQuery {
        @Override
        public List<SalesRepresentativeContact> activeRepresentatives() {
            return List.copyOf(representatives);
        }

        @Override
        public List<AdminSalesRepresentativeView> allRepresentatives() {
            return List.of();
        }

        @Override
        public AdminSalesRepresentativeView representative(SalesRepresentativeId id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<SalesRepresentativeContact> findContact(UUID id) {
            return representatives.stream().filter(r -> r.id().equals(id)).findFirst();
        }

        @Override
        public Optional<SalesRepresentativeContact> findActiveContact(UUID id) {
            return findContact(id);
        }
    }

    static final class InMemoryLineSheets implements LineSheetRepository {
        final Map<LineSheetId, LineSheet> store = new HashMap<>();

        @Override
        public Optional<LineSheet> findById(LineSheetId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<LineSheet> findByPublicId(UUID publicId) {
            return store.values().stream().filter(s -> s.publicId().equals(publicId)).findFirst();
        }

        @Override
        public void save(LineSheet lineSheet) {
            store.put(lineSheet.id(), lineSheet);
        }

        @Override
        public List<String> anonymizeOfBuyer(UUID buyerAccountId) {
            throw new UnsupportedOperationException("not used in these tests");
        }
    }

    static final class InMemoryStorage implements FileStorage {
        final Map<String, byte[]> files = new HashMap<>();

        @Override
        public void store(String key, InputStream content, long sizeBytes, String contentType) {
            try {
                files.put(key, content.readAllBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public Optional<StoredFile> open(String key) {
            return Optional.ofNullable(files.get(key))
                    .map(bytes -> new StoredFile(new ByteArrayInputStream(bytes), bytes.length, "application/pdf"));
        }

        @Override
        public void delete(String key) {
            files.remove(key);
        }
    }

    /** Symuluje granicę transakcji: akcje wykonują się dopiero przy commit()/rollback(). */
    static final class DeferredHooks implements TransactionalHooks {
        private final List<Runnable> onCommit = new ArrayList<>();
        private final List<Runnable> onRollback = new ArrayList<>();

        @Override
        public void afterCommit(Runnable action) {
            onCommit.add(action);
        }

        @Override
        public void afterRollback(Runnable action) {
            onRollback.add(action);
        }

        void commit() {
            List<Runnable> actions = List.copyOf(onCommit);
            onCommit.clear();
            onRollback.clear();
            actions.forEach(Runnable::run);
        }

        void rollback() {
            List<Runnable> actions = List.copyOf(onRollback);
            onCommit.clear();
            onRollback.clear();
            actions.forEach(Runnable::run);
        }
    }

    static final class RecordingAnalytics implements RecordAnalyticsEventsUseCase {
        final List<ServerEvent> serverEvents = new ArrayList<>();

        @Override
        public void recordClientEvents(List<ClientEvent> events) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void recordServerEvent(ServerEvent event) {
            serverEvents.add(event);
        }
    }
}
