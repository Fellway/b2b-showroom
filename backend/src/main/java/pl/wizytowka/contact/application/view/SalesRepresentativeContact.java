package pl.wizytowka.contact.application.view;

import java.util.UUID;

/** Dane kontaktowe opiekuna prezentowane klientom (strona publiczna, zestawienia PDF). */
public record SalesRepresentativeContact(UUID id, String fullName, String email, String phone) {
}
