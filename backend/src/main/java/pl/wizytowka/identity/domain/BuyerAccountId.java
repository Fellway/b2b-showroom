package pl.wizytowka.identity.domain;

import pl.wizytowka.shared.domain.Uuids;

import java.util.Objects;
import java.util.UUID;

public record BuyerAccountId(UUID value) {

    public BuyerAccountId {
        Objects.requireNonNull(value, "BuyerAccountId value");
    }

    public static BuyerAccountId newId() {
        return new BuyerAccountId(Uuids.v7());
    }

    public static BuyerAccountId of(UUID value) {
        return new BuyerAccountId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
