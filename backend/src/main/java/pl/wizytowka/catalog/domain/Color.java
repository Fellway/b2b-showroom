package pl.wizytowka.catalog.domain;

import java.util.Objects;

/** Słownik kolorów współdzielony przez produkty (ten sam "Navy" w wielu modelach). */
public final class Color {

    private final ColorId id;
    private ColorDetails details;
    private final long version;

    private Color(ColorId id, ColorDetails details, long version) {
        this.id = Objects.requireNonNull(id);
        this.details = Objects.requireNonNull(details);
        this.version = version;
    }

    public static Color create(ColorDetails details) {
        return new Color(ColorId.newId(), details, 0);
    }

    public static Color restore(ColorId id, ColorDetails details, long version) {
        return new Color(id, details, version);
    }

    public void update(ColorDetails details) {
        this.details = Objects.requireNonNull(details);
    }

    public ColorId id() {
        return id;
    }

    public ColorDetails details() {
        return details;
    }

    public long version() {
        return version;
    }
}
