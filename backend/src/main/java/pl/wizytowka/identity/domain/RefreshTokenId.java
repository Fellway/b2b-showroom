package pl.wizytowka.identity.domain;

import pl.wizytowka.shared.domain.Uuids;

import java.util.Objects;
import java.util.UUID;

public record RefreshTokenId(UUID value) {

    public RefreshTokenId {
        Objects.requireNonNull(value, "RefreshTokenId value");
    }

    public static RefreshTokenId newId() {
        return new RefreshTokenId(Uuids.v7());
    }

    public static RefreshTokenId of(UUID value) {
        return new RefreshTokenId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
