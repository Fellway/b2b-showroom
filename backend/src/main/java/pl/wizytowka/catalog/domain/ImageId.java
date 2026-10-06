package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.Uuids;

import java.util.Objects;
import java.util.UUID;

public record ImageId(UUID value) {

    public ImageId {
        Objects.requireNonNull(value, "ImageId value");
    }

    public static ImageId newId() {
        return new ImageId(Uuids.v7());
    }

    public static ImageId of(UUID value) {
        return new ImageId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
