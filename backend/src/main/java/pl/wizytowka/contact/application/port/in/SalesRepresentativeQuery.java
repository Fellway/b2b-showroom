package pl.wizytowka.contact.application.port.in;

import pl.wizytowka.contact.application.view.AdminSalesRepresentativeView;
import pl.wizytowka.contact.application.view.SalesRepresentativeContact;
import pl.wizytowka.contact.domain.SalesRepresentativeId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Odczyt opiekunów; metody z {@code UUID} służą innym modułom (np. zestawieniom PDF). */
public interface SalesRepresentativeQuery {

    /** Aktywni opiekunowie w kolejności prezentacji. */
    List<SalesRepresentativeContact> activeRepresentatives();

    List<AdminSalesRepresentativeView> allRepresentatives();

    /** @throws pl.wizytowka.shared.domain.ResourceNotFoundException */
    AdminSalesRepresentativeView representative(SalesRepresentativeId id);

    /** Kontakt niezależnie od statusu aktywności (np. do odtworzenia historycznego dokumentu). */
    Optional<SalesRepresentativeContact> findContact(UUID id);

    Optional<SalesRepresentativeContact> findActiveContact(UUID id);
}
