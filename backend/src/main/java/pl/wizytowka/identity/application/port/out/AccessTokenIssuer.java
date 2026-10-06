package pl.wizytowka.identity.application.port.out;

import pl.wizytowka.identity.application.port.in.AccessToken;
import pl.wizytowka.identity.domain.AdminUser;

import java.time.Instant;

/** Wystawia krótko żyjący access token (JWT) dla administratora. */
public interface AccessTokenIssuer {

    AccessToken issue(AdminUser user, Instant now);
}
