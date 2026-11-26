package pl.wizytowka.linesheet.adapter.in.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.wizytowka.identity.domain.BuyerAccountDeleted;
import pl.wizytowka.linesheet.application.port.in.ForgetBuyerUseCase;

/** Usunięcie konta kupca → usunięcie jego danych osobowych z zestawień, w tej samej transakcji (D25). */
@Component
class BuyerAccountDeletedListener {

    private final ForgetBuyerUseCase forgetBuyer;

    BuyerAccountDeletedListener(ForgetBuyerUseCase forgetBuyer) {
        this.forgetBuyer = forgetBuyer;
    }

    @EventListener
    void on(BuyerAccountDeleted event) {
        forgetBuyer.forgetBuyer(event.buyerAccountId());
    }
}
