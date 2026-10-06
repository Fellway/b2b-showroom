package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.Uuids;

import java.util.Objects;
import java.util.UUID;

public record ColorId(UUID value) {

    public ColorId {
        Objects.requireNonNull(value, "ColorId value");
    }

    public static ColorId newId() {
        return new ColorId(Uuids.v7());
    }

    public static ColorId of(UUID value) {
        return new ColorId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
