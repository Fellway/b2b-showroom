package pl.wizytowka.identity.domain;

import pl.wizytowka.shared.domain.Uuids;

import java.util.Objects;
import java.util.UUID;

public record AdminUserId(UUID value) {

    public AdminUserId {
        Objects.requireNonNull(value, "AdminUserId value");
    }

    public static AdminUserId newId() {
        return new AdminUserId(Uuids.v7());
    }

    public static AdminUserId of(UUID value) {
        return new AdminUserId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
