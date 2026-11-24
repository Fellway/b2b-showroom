package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.AdminUserId;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;

/**
 * Obsługa wniosków i kont kupców w panelu (ADMIN, EDITOR). Zmiany stanu przesyłają {@code version}
 * (optymistyczne blokowanie); akceptacja i odrzucenie wysyłają kupcowi e-mail.
 */
public interface ManageBuyersUseCase {

    PageResult<BuyerSummaryView> list(BuyerFilter filter, PageQuery page);

    /** @throws pl.wizytowka.shared.domain.ResourceNotFoundException */
    AdminBuyerView get(BuyerAccountId id);

    /** Liczba wniosków czekających na decyzję (licznik w nawigacji panelu). */
    long countPendingApproval();

    void approve(AdminUserId actor, BuyerAccountId id, long expectedVersion);

    /** @param note notatka wewnętrzna marki (nie trafia do kupca) */
    void reject(AdminUserId actor, BuyerAccountId id, long expectedVersion, String note);

    /** Zawieszenie kończy wszystkie sesje kupca. */
    void suspend(AdminUserId actor, BuyerAccountId id, long expectedVersion, String note);

    void reactivate(AdminUserId actor, BuyerAccountId id, long expectedVersion);

    /** Trwałe usunięcie konta (RODO); wygenerowane zestawienia zostają jako historyczne migawki. */
    void delete(BuyerAccountId id);
}
