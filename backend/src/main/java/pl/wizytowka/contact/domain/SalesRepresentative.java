package pl.wizytowka.contact.domain;

import java.util.Objects;

/** Opiekun handlowy prezentowany klientom B2B i dołączany do zestawień PDF. */
public final class SalesRepresentative {

    private final SalesRepresentativeId id;
    private SalesRepresentativeDetails details;
    private final long version;

    private SalesRepresentative(SalesRepresentativeId id, SalesRepresentativeDetails details, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.details = Objects.requireNonNull(details, "details");
        this.version = version;
    }

    public static SalesRepresentative create(SalesRepresentativeDetails details) {
        return new SalesRepresentative(SalesRepresentativeId.newId(), details, 0);
    }

    public static SalesRepresentative restore(SalesRepresentativeId id, SalesRepresentativeDetails details, long version) {
        return new SalesRepresentative(id, details, version);
    }

    public void update(SalesRepresentativeDetails details) {
        this.details = Objects.requireNonNull(details, "details");
    }

    public SalesRepresentativeId id() {
        return id;
    }

    public SalesRepresentativeDetails details() {
        return details;
    }

    public boolean isActive() {
        return details.active();
    }

    public long version() {
        return version;
    }
}
