package pl.wizytowka.shared.adapter.in.web.ratelimit;

/** Wynik sprawdzenia limitu dla jednego żądania. */
sealed interface RateLimitDecision {

    /** Żadna reguła nie dotyczy żądania. */
    record NotLimited() implements RateLimitDecision {
    }

    /** @param remaining najmniejsza liczba pozostałych żądań spośród pasujących reguł */
    record Allowed(long remaining) implements RateLimitDecision {
    }

    /** @param retryAfterSeconds po ilu sekundach kolejne żądanie zostanie przyjęte (co najmniej 1) */
    record Rejected(String rule, long retryAfterSeconds) implements RateLimitDecision {
    }
}
