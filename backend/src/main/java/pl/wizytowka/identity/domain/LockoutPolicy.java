package pl.wizytowka.identity.domain;

import java.time.Duration;
import java.util.Objects;

/** Ochrona przed zgadywaniem haseł: po {@code maxAttempts} nieudanych próbach konto blokuje się na {@code lockDuration}. */
public record LockoutPolicy(int maxAttempts, Duration lockDuration) {

    public LockoutPolicy {
        Objects.requireNonNull(lockDuration, "lockDuration");
        if (maxAttempts < 1 || lockDuration.isNegative() || lockDuration.isZero()) {
            throw new IllegalArgumentException("Lockout policy requires maxAttempts >= 1 and a positive duration");
        }
    }
}
