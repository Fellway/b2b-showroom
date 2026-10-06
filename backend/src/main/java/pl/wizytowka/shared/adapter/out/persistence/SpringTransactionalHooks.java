package pl.wizytowka.shared.adapter.out.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pl.wizytowka.shared.application.port.out.TransactionalHooks;

@Component
class SpringTransactionalHooks implements TransactionalHooks {

    private static final Logger log = LoggerFactory.getLogger(SpringTransactionalHooks.class);

    @Override
    public void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runSafely(action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runSafely(action);
            }
        });
    }

    @Override
    public void afterRollback(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    runSafely(action);
                }
            }
        });
    }

    /** Akcje po transakcji to sprzątanie — ich błąd nie może zmienić wyniku już zakończonej operacji. */
    private static void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.warn("Post-transaction action failed", e);
        }
    }
}
