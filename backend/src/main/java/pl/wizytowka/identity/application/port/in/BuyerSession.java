package pl.wizytowka.identity.application.port.in;

import java.time.Instant;

/**
 * Wynik logowania lub odświeżenia sesji kupca.
 * @param refreshToken surowa wartość refresh tokenu — trafia wyłącznie do ciasteczka httpOnly, nigdy do logów
 */
public record BuyerSession(AccessToken accessToken, String refreshToken, Instant refreshTokenExpiresAt,
                           BuyerAccountView buyer) {

    @Override
    public String toString() {
        return "BuyerSession[buyer=" + buyer.id() + ", accessTokenExpiresAt=" + accessToken.expiresAt() + "]";
    }
}
