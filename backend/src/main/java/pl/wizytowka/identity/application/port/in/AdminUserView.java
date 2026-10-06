package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.AdminRole;
import pl.wizytowka.identity.domain.AdminUser;

import java.time.Instant;
import java.util.UUID;

public record AdminUserView(UUID id, String email, String displayName, AdminRole role, boolean enabled,
                            Instant lockedUntil, Instant lastLoginAt, long version) {

    public static AdminUserView of(AdminUser user) {
        return new AdminUserView(user.id().value(), user.email(), user.displayName(), user.role(), user.isEnabled(),
                user.lockedUntil(), user.lastLoginAt(), user.version());
    }
}
