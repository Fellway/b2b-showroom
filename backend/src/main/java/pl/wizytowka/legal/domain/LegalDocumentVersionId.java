package pl.wizytowka.legal.domain;

import pl.wizytowka.shared.domain.Uuids;

import java.util.Objects;
import java.util.UUID;

public record LegalDocumentVersionId(UUID value) {

    public LegalDocumentVersionId {
        Objects.requireNonNull(value, "LegalDocumentVersionId value");
    }

    public static LegalDocumentVersionId newId() {
        return new LegalDocumentVersionId(Uuids.v7());
    }

    public static LegalDocumentVersionId of(UUID value) {
        return new LegalDocumentVersionId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
