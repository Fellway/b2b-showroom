package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.Uuids;

import java.util.Objects;
import java.util.UUID;

public record CategoryId(UUID value) {

    public CategoryId {
        Objects.requireNonNull(value, "CategoryId value");
    }

    public static CategoryId newId() {
        return new CategoryId(Uuids.v7());
    }

    public static CategoryId of(UUID value) {
        return new CategoryId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
