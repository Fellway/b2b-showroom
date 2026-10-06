package pl.wizytowka.identity.application.port.in;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {

    @Override
    public String toString() {
        return "AccessToken[expiresAt=" + expiresAt + "]";
    }
}
