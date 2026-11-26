package pl.wizytowka.legal.application.view;

import pl.wizytowka.legal.domain.LegalDocumentType;

import java.util.UUID;

/** Odwołanie do opublikowanej wersji — tyle zapisuje rejestr zgód kupców. */
public record LegalVersionRef(UUID id, LegalDocumentType type, int versionNumber) {
}
