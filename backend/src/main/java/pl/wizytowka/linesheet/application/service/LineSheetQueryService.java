package pl.wizytowka.linesheet.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.contact.application.port.in.SalesRepresentativeQuery;
import pl.wizytowka.contact.application.view.SalesRepresentativeContact;
import pl.wizytowka.linesheet.application.port.in.AdminLineSheetQuery;
import pl.wizytowka.linesheet.application.port.in.LineSheetStatisticsQuery;
import pl.wizytowka.linesheet.application.port.out.LineSheetReadModel;
import pl.wizytowka.linesheet.application.port.out.LineSheetRepository;
import pl.wizytowka.linesheet.application.view.LineSheetDetailView;
import pl.wizytowka.linesheet.application.view.LineSheetSummaryView;
import pl.wizytowka.linesheet.application.view.ProductLineSheetCount;
import pl.wizytowka.linesheet.domain.EmailDeliveryStatus;
import pl.wizytowka.linesheet.domain.LineSheet;
import pl.wizytowka.linesheet.domain.LineSheetId;
import pl.wizytowka.linesheet.domain.LineSheetItem;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
class LineSheetQueryService implements AdminLineSheetQuery, LineSheetStatisticsQuery {

    private static final int MAX_RANKING = 100;

    private final LineSheetRepository lineSheets;
    private final LineSheetReadModel readModel;
    private final SalesRepresentativeQuery salesRepresentatives;

    LineSheetQueryService(LineSheetRepository lineSheets, LineSheetReadModel readModel,
                          SalesRepresentativeQuery salesRepresentatives) {
        this.lineSheets = lineSheets;
        this.readModel = readModel;
        this.salesRepresentatives = salesRepresentatives;
    }

    @Override
    public PageResult<LineSheetSummaryView> lineSheets(EmailDeliveryStatus emailStatus, PageQuery page) {
        return readModel.findSummaries(emailStatus, page);
    }

    @Override
    public LineSheetDetailView lineSheet(LineSheetId id) {
        LineSheet sheet = lineSheets.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("linesheet.not-found", "Line sheet not found"));
        SalesRepresentativeContact salesRepresentative = Optional.ofNullable(sheet.salesRepresentativeId())
                .flatMap(salesRepresentatives::findContact)
                .orElse(null);
        BigDecimal wholesaleTotal = sheet.items().stream()
                .map(LineSheetItem::wholesaleTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(null);
        return new LineSheetDetailView(sheet.id().value(), sheet.publicId(), sheet.referenceNumber(), sheet.language(),
                sheet.currency().getCurrencyCode(), sheet.recipient(), sheet.message(), sheet.privacyAcceptedAt(),
                salesRepresentative, sheet.hasPdf(), sheet.pdfGeneratedAt(), sheet.emailStatus(), sheet.emailAttempts(),
                sheet.emailSentAt(), sheet.emailLastError(), sheet.createdAt(), sheet.items(), wholesaleTotal,
                sheet.version());
    }

    @Override
    public long countCreated(Instant from, Instant to) {
        requireRange(from, to);
        return readModel.countCreated(from, to);
    }

    @Override
    public List<ProductLineSheetCount> topProducts(Instant from, Instant to, int limit) {
        requireRange(from, to);
        if (limit < 1 || limit > MAX_RANKING) {
            throw new InvalidValueException("linesheet.statistics.limit", "Limit must be between 1 and " + MAX_RANKING);
        }
        return readModel.topProducts(from, to, limit);
    }

    private static void requireRange(Instant from, Instant to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isBefore(to)) {
            throw new InvalidValueException("linesheet.statistics.range", "'from' must be before 'to'");
        }
    }
}
