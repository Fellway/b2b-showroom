package pl.wizytowka.identity.application.port.out;

import pl.wizytowka.identity.application.port.in.BuyerFilter;
import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.BuyerStatus;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;

import java.util.Optional;

public interface BuyerAccountRepository {

    Optional<BuyerAccount> findById(BuyerAccountId id);

    Optional<BuyerAccount> findByEmail(String normalizedEmail);

    boolean existsByEmail(String normalizedEmail);

    /** @throws pl.wizytowka.shared.domain.ConcurrentModificationException przy niezgodności wersji */
    void save(BuyerAccount account);

    /** Trwałe usunięcie (RODO); sesje i tokeny usuwa baza, zestawienia zostają bez właściciela. */
    void delete(BuyerAccountId id);

    /** Najnowsze najpierw. */
    PageResult<BuyerAccount> find(BuyerFilter filter, PageQuery page);

    long countByStatus(BuyerStatus status);
}
