package pl.wizytowka.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.identity.application.port.in.AdminBuyerView;
import pl.wizytowka.identity.application.port.in.BuyerContactQuery;
import pl.wizytowka.identity.application.port.in.BuyerFilter;
import pl.wizytowka.identity.application.port.in.BuyerSummaryView;
import pl.wizytowka.identity.application.port.in.ManageBuyersUseCase;
import pl.wizytowka.identity.application.port.out.BuyerAccountRepository;
import pl.wizytowka.identity.application.port.out.BuyerMailer;
import pl.wizytowka.identity.application.port.out.BuyerRefreshTokenRepository;
import pl.wizytowka.identity.domain.AdminUserId;
import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.BuyerStatus;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.application.port.out.TransactionalHooks;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Decyzje marki o kontach kupców (panel) oraz dane kontaktowe aktywnych kupców dla innych modułów. */
@Service
class BuyerAdministrationService implements ManageBuyersUseCase, BuyerContactQuery {

    private final BuyerAccountRepository accounts;
    private final BuyerRefreshTokenRepository sessions;
    private final BuyerMailer mailer;
    private final TransactionalHooks transactionalHooks;
    private final Clock clock;

    BuyerAdministrationService(BuyerAccountRepository accounts, BuyerRefreshTokenRepository sessions, BuyerMailer mailer,
                               TransactionalHooks transactionalHooks, Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.mailer = mailer;
        this.transactionalHooks = transactionalHooks;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BuyerSummaryView> list(BuyerFilter filter, PageQuery page) {
        return accounts.find(filter == null ? BuyerFilter.all() : filter, page).map(BuyerSummaryView::of);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBuyerView get(BuyerAccountId id) {
        return AdminBuyerView.of(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingApproval() {
        return accounts.countByStatus(BuyerStatus.PENDING_APPROVAL);
    }

    @Override
    @Transactional
    public void approve(AdminUserId actor, BuyerAccountId id, long expectedVersion) {
        BuyerAccount account = loadForChange(id, expectedVersion);
        account.approve(actor, clock.instant());
        accounts.save(account);
        transactionalHooks.afterCommit(() -> mailer.sendAccountApproved(account.email(), account.language()));
    }

    @Override
    @Transactional
    public void reject(AdminUserId actor, BuyerAccountId id, long expectedVersion, String note) {
        BuyerAccount account = loadForChange(id, expectedVersion);
        account.reject(actor, note, clock.instant());
        accounts.save(account);
        transactionalHooks.afterCommit(() -> mailer.sendAccountRejected(account.email(), account.language()));
    }

    @Override
    @Transactional
    public void suspend(AdminUserId actor, BuyerAccountId id, long expectedVersion, String note) {
        Instant now = clock.instant();
        BuyerAccount account = loadForChange(id, expectedVersion);
        account.suspend(actor, note, now);
        accounts.save(account);
        sessions.revokeAllOfBuyer(account.id(), now);
    }

    @Override
    @Transactional
    public void reactivate(AdminUserId actor, BuyerAccountId id, long expectedVersion) {
        BuyerAccount account = loadForChange(id, expectedVersion);
        account.reactivate(actor, clock.instant());
        accounts.save(account);
    }

    @Override
    @Transactional
    public void delete(BuyerAccountId id) {
        load(id);
        accounts.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BuyerContact> activeBuyer(UUID buyerAccountId) {
        if (buyerAccountId == null) {
            return Optional.empty();
        }
        return accounts.findById(BuyerAccountId.of(buyerAccountId))
                .filter(BuyerAccount::isActive)
                .map(a -> new BuyerContact(a.id().value(), a.companyName(), a.contactName(), a.email(), a.phone(),
                        a.taxId().display(), a.language()));
    }

    private BuyerAccount loadForChange(BuyerAccountId id, long expectedVersion) {
        BuyerAccount account = load(id);
        BuyerAuthenticationService.requireVersion(account, expectedVersion);
        return account;
    }

    private BuyerAccount load(BuyerAccountId id) {
        return accounts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("identity.buyer.not-found", "Buyer account not found"));
    }
}
