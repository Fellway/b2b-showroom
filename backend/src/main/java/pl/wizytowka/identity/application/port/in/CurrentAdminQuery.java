package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.AdminUserId;

public interface CurrentAdminQuery {

    /** @throws pl.wizytowka.shared.domain.ResourceNotFoundException */
    AdminUserView me(AdminUserId adminUserId);
}
