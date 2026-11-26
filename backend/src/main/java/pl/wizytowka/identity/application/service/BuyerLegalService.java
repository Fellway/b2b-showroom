package pl.wizytowka.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.identity.application.port.in.BuyerConsentView;
import pl.wizytowka.identity.application.port.in.BuyerLegalUseCase;
import pl.wizytowka.identity.application.port.out.BuyerAccountRepository;
import pl.wizytowka.identity.application.port.out.BuyerConsentRepository;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.BuyerConsent;
import pl.wizytowka.legal.application.port.in.LegalDocumentQuery;
import pl.wizytowka.legal.application.view.LegalRequirement;
import pl.wizytowka.legal.application.view.LegalVersionRef;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.ResourceConflictException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Rejestr zgód kupców (D25) i brama regulaminu dla cen i zestawień. */
@Service
class BuyerLegalService implements BuyerLegalUseCase {

    static final String VERSION_OUTDATED = "identity.buyer.legal-version-outdated";

    private final BuyerAccountRepository accounts;
    private final BuyerConsentRepository consents;
    private final LegalDocumentQuery legal;
    private final Clock clock;

    BuyerLegalService(BuyerAccountRepository accounts, BuyerConsentRepository consents, LegalDocumentQuery legal,
                      Clock clock) {
        this.accounts = accounts;
        this.consents = consents;
        this.legal = legal;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public BuyerLegalStatus status(BuyerAccountId buyer) {
        requireExists(buyer);
        List<BuyerConsent> history = consents.findByBuyer(buyer);
        List<PendingDocument> pending = legal.requirements().stream()
                .filter(requirement -> !consents.hasAccepted(buyer, requirement.type(), requirement.minimumVersionNumber()))
                .map(requirement -> new PendingDocument(requirement.type(), requirement.currentVersionId(),
                        requirement.currentVersionNumber(), requirement.blocking(),
                        history.stream().noneMatch(c -> c.documentType() == requirement.type())))
                .toList();
        return new BuyerLegalStatus(pending, history.stream().map(BuyerConsentView::of).toList());
    }

    @Override
    @Transactional
    public void accept(BuyerAccountId buyer, List<UUID> versionIds, CatalogLanguage language) {
        requireExists(buyer);
        if (versionIds == null || versionIds.isEmpty()) {
            throw new InvalidValueException("identity.buyer.legal-versions-required", "No document version given");
        }
        Instant now = clock.instant();
        List<UUID> alreadyAccepted = consents.findByBuyer(buyer).stream().map(BuyerConsent::documentVersionId).toList();
        for (UUID versionId : new LinkedHashSet<>(versionIds)) {
            LegalVersionRef ref = currentVersion(versionId);
            if (!alreadyAccepted.contains(versionId)) {
                consents.save(BuyerConsent.record(buyer, ref.type(), ref.id(), ref.versionNumber(), language,
                        BuyerConsent.Context.UPDATE, now));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean mustAcceptTerms(UUID buyerAccountId) {
        return legal.requirement(LegalDocumentType.TERMS)
                .filter(LegalRequirement::blocking)
                .map(requirement -> !consents.hasAccepted(BuyerAccountId.of(buyerAccountId), LegalDocumentType.TERMS,
                        requirement.minimumVersionNumber()))
                .orElse(false);
    }

    /**
     * Rejestracja: kupiec musi zaakceptować obowiązujące wersje wszystkich opublikowanych dokumentów — dokładnie te,
     * które widział w formularzu (gdy w międzyczasie opublikowano nowszą, formularz musi ją pokazać).
     */
    Map<LegalDocumentType, LegalVersionRef> requireCurrentForRegistration(UUID termsVersionId, UUID privacyVersionId) {
        Map<LegalDocumentType, UUID> given = new EnumMap<>(LegalDocumentType.class);
        if (termsVersionId != null) given.put(LegalDocumentType.TERMS, termsVersionId);
        if (privacyVersionId != null) given.put(LegalDocumentType.PRIVACY_POLICY, privacyVersionId);
        Map<LegalDocumentType, LegalVersionRef> accepted = new EnumMap<>(LegalDocumentType.class);
        for (LegalRequirement requirement : legal.requirements()) {
            if (!Objects.equals(given.get(requirement.type()), requirement.currentVersionId())) {
                throw outdated();
            }
            accepted.put(requirement.type(), new LegalVersionRef(requirement.currentVersionId(), requirement.type(),
                    requirement.currentVersionNumber()));
        }
        return accepted;
    }

    void recordRegistration(BuyerAccountId buyer, Map<LegalDocumentType, LegalVersionRef> accepted,
                            CatalogLanguage language, Instant now) {
        accepted.values().forEach(ref -> consents.save(BuyerConsent.record(buyer, ref.type(), ref.id(),
                ref.versionNumber(), language, BuyerConsent.Context.REGISTRATION, now)));
    }

    List<BuyerConsentView> history(BuyerAccountId buyer) {
        return consents.findByBuyer(buyer).stream().map(BuyerConsentView::of).toList();
    }

    private LegalVersionRef currentVersion(UUID versionId) {
        LegalVersionRef ref = legal.publishedVersionRef(versionId)
                .orElseThrow(() -> new InvalidValueException("identity.buyer.legal-version-unknown",
                        "Unknown document version"));
        boolean current = legal.requirement(ref.type())
                .map(requirement -> requirement.currentVersionId().equals(ref.id()))
                .orElse(false);
        if (!current) {
            throw outdated();
        }
        return ref;
    }

    private void requireExists(BuyerAccountId buyer) {
        if (accounts.findById(buyer).isEmpty()) {
            throw new ResourceNotFoundException("identity.buyer.not-found", "Buyer account not found");
        }
    }

    private static ResourceConflictException outdated() {
        return new ResourceConflictException(VERSION_OUTDATED,
                "A newer version of the terms or privacy policy has been published; review it and try again");
    }
}
