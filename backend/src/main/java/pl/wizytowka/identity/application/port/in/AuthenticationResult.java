package pl.wizytowka.identity.application.port.in;

import java.time.Instant;

/**
 * Wynik logowania lub odświeżenia sesji.
 * @param refreshToken surowa wartość refresh tokenu — trafia wyłącznie do ciasteczka httpOnly, nigdy do logów
 */
public record AuthenticationResult(AccessToken accessToken, String refreshToken, Instant refreshTokenExpiresAt,
                                   AdminUserView admin) {

    @Override
    public String toString() {
        return "AuthenticationResult[admin=" + admin.email() + ", accessTokenExpiresAt=" + accessToken.expiresAt() + "]";
    }
}
