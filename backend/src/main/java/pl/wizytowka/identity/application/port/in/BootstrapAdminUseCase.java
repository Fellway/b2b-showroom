package pl.wizytowka.identity.application.port.in;

public interface BootstrapAdminUseCase {

    /** Tworzy pierwsze konto ADMIN, jeśli w systemie nie ma żadnego administratora. @return czy utworzono konto */
    boolean createFirstAdminIfNoneExists(String email, String password);
}
