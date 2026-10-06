package pl.wizytowka.linesheet.domain;

import pl.wizytowka.shared.domain.Uuids;

import java.util.Objects;
import java.util.UUID;

/** Wewnętrzny identyfikator zestawienia (UUIDv7). Do linków publicznych służy osobny, losowy {@code publicId}. */
public record LineSheetId(UUID value) {

    public LineSheetId {
        Objects.requireNonNull(value, "LineSheetId value");
    }

    public static LineSheetId newId() {
        return new LineSheetId(Uuids.v7());
    }

    public static LineSheetId of(UUID value) {
        return new LineSheetId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
