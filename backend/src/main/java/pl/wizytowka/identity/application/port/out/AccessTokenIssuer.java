package pl.wizytowka.identity.application.port.out;

import pl.wizytowka.identity.application.port.in.AccessToken;
import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.BuyerAccount;

import java.time.Instant;

/** Wystawia krótko żyjący access token (JWT) dla administratora albo kupca (rola {@code BUYER}). */
public interface AccessTokenIssuer {

    AccessToken issue(AdminUser user, Instant now);

    AccessToken issue(BuyerAccount buyer, Instant now);
}
