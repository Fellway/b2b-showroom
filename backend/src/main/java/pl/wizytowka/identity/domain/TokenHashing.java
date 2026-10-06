package pl.wizytowka.identity.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hash refresh tokenu (SHA-256, hex). Wystarcza zwykły skrót — token ma 256 bitów entropii,
 * więc nie potrzeba soli ani wolnej funkcji jak przy hasłach.
 */
public final class TokenHashing {

    private TokenHashing() {
    }

    public static String sha256Hex(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
