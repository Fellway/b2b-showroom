package pl.wizytowka.shared.domain;

import java.security.SecureRandom;
import java.util.UUID;

/** Generator UUIDv7 (RFC 9562): 48 bitów znacznika czasu + 74 bity losowe. Rosnące w czasie, więc przyjazne indeksom. */
public final class Uuids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Uuids() {
    }

    public static UUID v7() {
        long timestamp = System.currentTimeMillis();
        long randA = RANDOM.nextInt(1 << 12);
        long randB = RANDOM.nextLong();
        long mostSignificant = (timestamp << 16) | (0x7L << 12) | randA;
        long leastSignificant = (randB & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(mostSignificant, leastSignificant);
    }
}
