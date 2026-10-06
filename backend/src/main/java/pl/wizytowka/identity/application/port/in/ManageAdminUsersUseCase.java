package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.AdminRole;
import pl.wizytowka.identity.domain.AdminUserId;

import java.util.List;

/**
 * Zarządzanie kontami panelu (tylko rola ADMIN). {@code actor} to zalogowany administrator —
 * nie może wyłączyć ani zdegradować samego siebie; system zawsze zachowuje co najmniej jednego aktywnego ADMIN-a.
 */
public interface ManageAdminUsersUseCase {

    List<AdminUserView> list();

    AdminUserId create(String email, String displayName, AdminRole role, String initialPassword);

    void update(AdminUserId actor, AdminUserId id, long expectedVersion, String displayName, AdminRole role);

    /** Wyłączenie konta unieważnia wszystkie jego sesje (refresh tokeny). */
    void setEnabled(AdminUserId actor, AdminUserId id, boolean enabled);

    /** Ustawia nowe hasło, zdejmuje blokadę i unieważnia wszystkie sesje użytkownika. */
    void resetPassword(AdminUserId id, String newPassword);
}
