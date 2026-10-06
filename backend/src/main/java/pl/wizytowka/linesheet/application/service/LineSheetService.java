package pl.wizytowka.linesheet.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.analytics.application.port.in.RecordAnalyticsEventsUseCase;
import pl.wizytowka.analytics.application.port.in.ServerEvent;
import pl.wizytowka.analytics.domain.AnalyticsEventType;
import pl.wizytowka.catalog.application.port.in.VariantSnapshotQuery;
import pl.wizytowka.catalog.application.view.VariantSnapshot;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.contact.application.port.in.SalesRepresentativeQuery;
import pl.wizytowka.contact.application.view.SalesRepresentativeContact;
import pl.wizytowka.linesheet.application.port.in.CreateLineSheetCommand;
import pl.wizytowka.linesheet.application.port.in.CreateLineSheetUseCase;
import pl.wizytowka.linesheet.application.port.in.GetLineSheetPdfUseCase;
import pl.wizytowka.linesheet.application.port.in.ResendLineSheetEmailUseCase;
import pl.wizytowka.linesheet.application.port.out.LineSheetDocumentRenderer;
import pl.wizytowka.linesheet.application.port.out.LineSheetNumberGenerator;
import pl.wizytowka.linesheet.application.port.out.LineSheetRepository;
import pl.wizytowka.linesheet.application.view.CreatedLineSheet;
import pl.wizytowka.linesheet.application.view.LineSheetDocument;
import pl.wizytowka.linesheet.application.view.LineSheetPdf;
import pl.wizytowka.linesheet.domain.LineSheet;
import pl.wizytowka.linesheet.domain.LineSheetId;
import pl.wizytowka.linesheet.domain.Recipient;
import pl.wizytowka.linesheet.domain.SelectedVariant;
import pl.wizytowka.shared.application.port.out.FileStorage;
import pl.wizytowka.shared.application.port.out.TransactionalHooks;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
class LineSheetService implements CreateLineSheetUseCase, GetLineSheetPdfUseCase, ResendLineSheetEmailUseCase {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final LineSheetRepository lineSheets;
    private final LineSheetNumberGenerator numbers;
    private final LineSheetDocumentRenderer renderer;
    private final LineSheetEmailDispatcher emailDispatcher;
    private final VariantSnapshotQuery variantSnapshots;
    private final SalesRepresentativeQuery salesRepresentatives;
    private final RecordAnalyticsEventsUseCase analytics;
    private final FileStorage fileStorage;
    private final TransactionalHooks transactionalHooks;
    private final Clock clock;

    LineSheetService(LineSheetRepository lineSheets, LineSheetNumberGenerator numbers, LineSheetDocumentRenderer renderer,
                     LineSheetEmailDispatcher emailDispatcher, VariantSnapshotQuery variantSnapshots,
                     SalesRepresentativeQuery salesRepresentatives, RecordAnalyticsEventsUseCase analytics,
                     FileStorage fileStorage, TransactionalHooks transactionalHooks, Clock clock) {
        this.lineSheets = lineSheets;
        this.numbers = numbers;
        this.renderer = renderer;
        this.emailDispatcher = emailDispatcher;
        this.variantSnapshots = variantSnapshots;
        this.salesRepresentatives = salesRepresentatives;
        this.analytics = analytics;
        this.fileStorage = fileStorage;
        this.transactionalHooks = transactionalHooks;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreatedLineSheet create(CreateLineSheetCommand command) {
        List<SelectedVariant> selections = resolveSelections(command);
        SalesRepresentativeContact salesRepresentative = resolveSalesRepresentative(command.salesRepresentativeId());
        Instant now = clock.instant();
        String referenceNumber = LineSheet.formatReference(now.atZone(ZoneOffset.UTC).getYear(), numbers.nextSequenceNumber());
        Recipient recipient = new Recipient(command.companyName(), command.contactName(), command.email(),
                command.phone(), command.taxId());

        LineSheet sheet = LineSheet.create(referenceNumber, command.language(), recipient, command.message(),
                command.privacyAccepted(), command.sendEmail(),
                salesRepresentative == null ? null : salesRepresentative.id(), command.visitorId(), selections, now);
        LineSheetDocument document = LineSheetDocuments.of(sheet, salesRepresentative);
        byte[] pdf = storePdf(sheet, document);
        sheet.attachPdf(LineSheetDocuments.storageKey(sheet), now);
        if (command.sendEmail()) {
            sheet.requestEmailDelivery();
        }
        lineSheets.save(sheet);

        if (command.sendEmail()) {
            transactionalHooks.afterCommit(() -> emailDispatcher.deliver(sheet.id(), document, pdf));
        }
        transactionalHooks.afterCommit(() -> analytics.recordServerEvent(new ServerEvent(
                AnalyticsEventType.LINE_SHEET_GENERATED, command.visitorId(), command.language(), null, null,
                sheet.id().value(), Map.of("items", String.valueOf(sheet.items().size())))));
        return new CreatedLineSheet(sheet.publicId(), sheet.referenceNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public LineSheetPdf pdf(UUID publicId) {
        LineSheet sheet = lineSheets.findByPublicId(publicId).orElseThrow(LineSheetService::notFound);
        if (!sheet.hasPdf()) {
            throw pdfNotFound();
        }
        FileStorage.StoredFile file = fileStorage.open(sheet.pdfStorageKey()).orElseThrow(LineSheetService::pdfNotFound);
        return new LineSheetPdf(file.content(), file.sizeBytes(), sheet.referenceNumber() + ".pdf");
    }

    @Override
    @Transactional
    public void resend(LineSheetId id) {
        LineSheet sheet = lineSheets.findById(id).orElseThrow(LineSheetService::notFound);
        sheet.requestEmailDelivery();
        SalesRepresentativeContact salesRepresentative = Optional.ofNullable(sheet.salesRepresentativeId())
                .flatMap(salesRepresentatives::findContact)
                .orElse(null);
        LineSheetDocument document = LineSheetDocuments.of(sheet, salesRepresentative);
        byte[] pdf = readPdf(sheet).orElseGet(() -> storePdf(sheet, document));
        lineSheets.save(sheet);
        transactionalHooks.afterCommit(() -> emailDispatcher.deliver(sheet.id(), document, pdf));
    }

    private List<SelectedVariant> resolveSelections(CreateLineSheetCommand command) {
        List<CreateLineSheetCommand.Selection> requested = command.selections();
        if (requested.isEmpty()) {
            throw new InvalidValueException("linesheet.items.empty", "Line sheet requires at least one item");
        }
        if (requested.size() > LineSheet.MAX_ITEMS) {
            throw new InvalidValueException("linesheet.items.too-many", "Line sheet can contain at most " + LineSheet.MAX_ITEMS + " items");
        }
        Map<UUID, Integer> quantities = new HashMap<>();
        for (CreateLineSheetCommand.Selection selection : requested) {
            if (quantities.containsKey(selection.variantId())) {
                throw new InvalidValueException("linesheet.items.duplicate", "Each variant can be selected only once");
            }
            quantities.put(selection.variantId(), selection.quantity());
        }
        List<VariantId> variantIds = requested.stream().map(s -> VariantId.of(s.variantId())).toList();
        return variantSnapshots.selectableVariants(variantIds, command.language()).stream()
                .map(snapshot -> toSelection(snapshot, quantities.get(snapshot.variantId().value())))
                .toList();
    }

    private static SelectedVariant toSelection(VariantSnapshot snapshot, Integer quantity) {
        return new SelectedVariant(snapshot.productId().value(), snapshot.variantId().value(), snapshot.styleCode(),
                snapshot.productName(), snapshot.colorName(), snapshot.sizeCode(), snapshot.sku().value(),
                snapshot.ean().value(), snapshot.pricing(), snapshot.imageStorageKey(), quantity);
    }

    private SalesRepresentativeContact resolveSalesRepresentative(UUID requestedId) {
        if (requestedId != null) {
            return salesRepresentatives.findActiveContact(requestedId)
                    .orElseThrow(() -> new InvalidValueException("linesheet.sales-representative.unknown",
                            "Selected sales representative is not available"));
        }
        return salesRepresentatives.activeRepresentatives().stream().findFirst().orElse(null);
    }

    /** Renderuje i zapisuje PDF; przy wycofaniu transakcji plik zostanie usunięty. */
    private byte[] storePdf(LineSheet sheet, LineSheetDocument document) {
        byte[] pdf = renderer.render(document);
        String key = LineSheetDocuments.storageKey(sheet);
        fileStorage.store(key, new ByteArrayInputStream(pdf), pdf.length, PDF_CONTENT_TYPE);
        transactionalHooks.afterRollback(() -> fileStorage.delete(key));
        return pdf;
    }

    private Optional<byte[]> readPdf(LineSheet sheet) {
        Optional<FileStorage.StoredFile> stored = fileStorage.open(sheet.pdfStorageKey());
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        try (FileStorage.StoredFile file = stored.get()) {
            return Optional.of(file.content().readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read line sheet PDF", e);
        }
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("linesheet.not-found", "Line sheet not found");
    }

    private static ResourceNotFoundException pdfNotFound() {
        return new ResourceNotFoundException("linesheet.pdf.not-found", "Line sheet PDF is not available");
    }
}
