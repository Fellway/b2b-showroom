package pl.wizytowka.identity.adapter.out.persistence;

import org.springframework.stereotype.Component;
import pl.wizytowka.identity.application.port.out.BuyerConsentRepository;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.BuyerConsent;
import pl.wizytowka.legal.domain.LegalDocumentType;

import java.util.List;

@Component
class BuyerConsentPersistenceAdapter implements BuyerConsentRepository {

    private final BuyerConsentJpaRepository repository;

    BuyerConsentPersistenceAdapter(BuyerConsentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(BuyerConsent consent) {
        BuyerConsentJpaEntity entity = new BuyerConsentJpaEntity();
        entity.id = consent.id();
        entity.buyerAccountId = consent.buyerAccountId().value();
        entity.documentType = consent.documentType();
        entity.documentVersionId = consent.documentVersionId();
        entity.versionNumber = consent.versionNumber();
        entity.language = consent.language();
        entity.context = consent.context();
        entity.acceptedAt = consent.acceptedAt();
        repository.save(entity);
    }

    @Override
    public List<BuyerConsent> findByBuyer(BuyerAccountId buyer) {
        return repository.findByBuyerAccountIdOrderByAcceptedAtDescIdDesc(buyer.value()).stream()
                .map(e -> new BuyerConsent(e.id, BuyerAccountId.of(e.buyerAccountId), e.documentType,
                        e.documentVersionId, e.versionNumber, e.language, e.context, e.acceptedAt))
                .toList();
    }

    @Override
    public boolean hasAccepted(BuyerAccountId buyer, LegalDocumentType type, int minimumVersionNumber) {
        return repository.hasAccepted(buyer.value(), type, minimumVersionNumber);
    }
}
