package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.Uuids;

import java.util.Objects;
import java.util.UUID;

public record VariantId(UUID value) {

    public VariantId {
        Objects.requireNonNull(value, "VariantId value");
    }

    public static VariantId newId() {
        return new VariantId(Uuids.v7());
    }

    public static VariantId of(UUID value) {
        return new VariantId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
