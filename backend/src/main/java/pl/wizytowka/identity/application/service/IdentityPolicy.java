package pl.wizytowka.identity.application.service;

import pl.wizytowka.identity.domain.LockoutPolicy;

import java.time.Duration;
import java.util.Objects;

/** Parametry bezpieczeństwa sesji przekazywane z konfiguracji. */
public record IdentityPolicy(LockoutPolicy lockout, Duration refreshTokenTtl) {

    public IdentityPolicy {
        Objects.requireNonNull(lockout, "lockout");
        Objects.requireNonNull(refreshTokenTtl, "refreshTokenTtl");
    }
}
