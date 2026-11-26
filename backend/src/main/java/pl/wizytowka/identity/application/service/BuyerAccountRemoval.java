package pl.wizytowka.identity.application.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import pl.wizytowka.identity.application.port.out.BuyerAccountRepository;
import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerAccountDeleted;

/**
 * Usunięcie konta kupca — wspólne dla kupca („Moje konto”) i marki (panel). Sesje, tokeny z e-maili i rejestr zgód
 * usuwa baza (kaskada); zdarzenie {@link BuyerAccountDeleted} pozwala innym modułom usunąć dane osobowe kupca
 * w tej samej transakcji (słuchacze synchroniczni).
 */
@Component
class BuyerAccountRemoval {

    private final BuyerAccountRepository accounts;
    private final ApplicationEventPublisher events;

    BuyerAccountRemoval(BuyerAccountRepository accounts, ApplicationEventPublisher events) {
        this.accounts = accounts;
        this.events = events;
    }

    void remove(BuyerAccount account) {
        events.publishEvent(new BuyerAccountDeleted(account.id().value()));
        accounts.delete(account.id());
    }
}
