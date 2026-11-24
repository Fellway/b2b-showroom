package pl.wizytowka.identity.application.service;

import pl.wizytowka.identity.domain.LockoutPolicy;

import java.time.Duration;
import java.util.Objects;

/**
 * Parametry bezpieczeństwa kont kupców przekazywane z konfiguracji.
 *
 * @param refreshTokenTtl      długość sesji kupca (dłuższa niż w panelu — kupiec wraca do katalogu co kilka dni)
 * @param emailVerificationTtl ważność linku potwierdzającego adres
 * @param passwordResetTtl     ważność linku do ustawienia nowego hasła
 */
public record BuyerPolicy(LockoutPolicy lockout, Duration refreshTokenTtl, Duration emailVerificationTtl,
                          Duration passwordResetTtl) {

    public BuyerPolicy {
        Objects.requireNonNull(lockout, "lockout");
        Objects.requireNonNull(refreshTokenTtl, "refreshTokenTtl");
        Objects.requireNonNull(emailVerificationTtl, "emailVerificationTtl");
        Objects.requireNonNull(passwordResetTtl, "passwordResetTtl");
    }
}
