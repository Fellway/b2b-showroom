package pl.wizytowka.contact.domain;

import pl.wizytowka.shared.domain.Uuids;

import java.util.Objects;
import java.util.UUID;

public record SalesRepresentativeId(UUID value) {

    public SalesRepresentativeId {
        Objects.requireNonNull(value, "SalesRepresentativeId value");
    }

    public static SalesRepresentativeId newId() {
        return new SalesRepresentativeId(Uuids.v7());
    }

    public static SalesRepresentativeId of(UUID value) {
        return new SalesRepresentativeId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
