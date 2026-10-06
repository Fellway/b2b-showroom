package pl.wizytowka.shared.application.port.out;

/**
 * Akcje uboczne powiązane z wynikiem bieżącej transakcji — np. usunięcie pliku z magazynu
 * dopiero po zatwierdzeniu zmian w bazie albo sprzątnięcie wgranego pliku po wycofaniu.
 * Bez aktywnej transakcji {@link #afterCommit} wykonuje akcję natychmiast, a {@link #afterRollback} ją pomija.
 */
public interface TransactionalHooks {

    void afterCommit(Runnable action);

    void afterRollback(Runnable action);
}
