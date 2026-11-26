package pl.wizytowka.linesheet.adapter.out.persistence;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import pl.wizytowka.linesheet.application.port.out.LineSheetNumberGenerator;
import pl.wizytowka.linesheet.application.port.out.LineSheetReadModel;
import pl.wizytowka.linesheet.application.port.out.LineSheetRepository;
import pl.wizytowka.linesheet.application.view.LineSheetSummaryView;
import pl.wizytowka.linesheet.application.view.ProductLineSheetCount;
import pl.wizytowka.linesheet.domain.LineSheet;
import pl.wizytowka.linesheet.domain.LineSheetId;
import pl.wizytowka.linesheet.domain.Recipient;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.application.Versions;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class LineSheetPersistenceAdapter implements LineSheetRepository, LineSheetReadModel, LineSheetNumberGenerator {

    private final LineSheetJpaRepository repository;

    LineSheetPersistenceAdapter(LineSheetJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<LineSheet> findById(LineSheetId id) {
        return repository.findById(id.value()).map(LineSheetPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<LineSheet> findByPublicId(UUID publicId) {
        return repository.findByPublicId(publicId).map(LineSheetPersistenceAdapter::toDomain);
    }

    @Override
    public void save(LineSheet sheet) {
        Optional<LineSheetJpaEntity> existing = repository.findById(sheet.id().value());
        LineSheetJpaEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            Versions.require(sheet.version(), entity.version);
        } else {
            entity = newEntity(sheet);
        }
        entity.pdfStorageKey = sheet.pdfStorageKey();
        entity.pdfGeneratedAt = sheet.pdfGeneratedAt();
        repository.save(entity);
    }

    @Override
    public List<String> anonymizeOfBuyer(UUID buyerAccountId) {
        List<String> pdfKeys = repository.pdfKeysOfBuyer(buyerAccountId);
        repository.anonymizeOfBuyer(buyerAccountId);
        return pdfKeys;
    }
    @Override
    public PageResult<LineSheetSummaryView> findSummaries(PageQuery page) {
        Page<LineSheetSummaryView> result = repository.findSummaries(PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent(), page.page(), page.size(), result.getTotalElements());
    }

    @Override
    public PageResult<LineSheetSummaryView> findSummariesOfBuyer(UUID buyerAccountId, PageQuery page) {
        Page<LineSheetSummaryView> result = repository.findSummariesOfBuyer(buyerAccountId,
                PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent(), page.page(), page.size(), result.getTotalElements());
    }

    @Override
    public long countCreated(Instant from, Instant to) {
        return repository.countCreated(from, to);
    }

    @Override
    public List<ProductLineSheetCount> topProducts(Instant from, Instant to, int limit) {
        return repository.topProducts(from, to, Limit.of(limit));
    }

    @Override
    public long nextSequenceNumber() {
        return repository.nextSequenceNumber();
    }

    private static LineSheetJpaEntity newEntity(LineSheet sheet) {
        LineSheetJpaEntity entity = new LineSheetJpaEntity(sheet.id().value());
        entity.publicId = sheet.publicId();
        entity.referenceNumber = sheet.referenceNumber();
        entity.language = sheet.language();
        entity.currency = sheet.currency().getCurrencyCode();
        Recipient recipient = sheet.recipient();
        entity.companyName = recipient.companyName();
        entity.contactName = recipient.contactName();
        entity.email = recipient.email();
        entity.phone = recipient.phone();
        entity.taxId = recipient.taxId();
        entity.message = sheet.message();
        entity.privacyAcceptedAt = sheet.privacyAcceptedAt();
        entity.salesRepresentativeId = sheet.salesRepresentativeId();
        entity.visitorId = sheet.visitorId();
        entity.buyerAccountId = sheet.buyerAccountId();
        entity.createdAt = sheet.createdAt();
        sheet.items().forEach(item -> entity.items.add(LineSheetItemJpaEntity.of(entity, item)));
        return entity;
    }

    private static LineSheet toDomain(LineSheetJpaEntity e) {
        return LineSheet.restore()
                .id(LineSheetId.of(e.id))
                .publicId(e.publicId)
                .referenceNumber(e.referenceNumber)
                .language(e.language)
                .currency(Currency.getInstance(e.currency))
                .recipient(new Recipient(e.companyName, e.contactName, e.email, e.phone, e.taxId))
                .message(e.message)
                .privacyAcceptedAt(e.privacyAcceptedAt)
                .salesRepresentativeId(e.salesRepresentativeId)
                .visitorId(e.visitorId)
                .buyerAccountId(e.buyerAccountId)
                .items(e.items.stream().map(LineSheetItemJpaEntity::toDomain).toList())
                .createdAt(e.createdAt)
                .pdf(e.pdfStorageKey, e.pdfGeneratedAt)
                .version(e.version)
                .build();
    }
}
