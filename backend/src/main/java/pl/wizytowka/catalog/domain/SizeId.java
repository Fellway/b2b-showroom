package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.Uuids;

import java.util.Objects;
import java.util.UUID;

public record SizeId(UUID value) {

    public SizeId {
        Objects.requireNonNull(value, "SizeId value");
    }

    public static SizeId newId() {
        return new SizeId(Uuids.v7());
    }

    public static SizeId of(UUID value) {
        return new SizeId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
