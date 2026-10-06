package pl.wizytowka.contact.application.port.in;

import pl.wizytowka.contact.domain.SalesRepresentativeDetails;
import pl.wizytowka.contact.domain.SalesRepresentativeId;

public interface ManageSalesRepresentativesUseCase {

    /** @throws pl.wizytowka.shared.domain.ResourceConflictException gdy e-mail jest już używany */
    SalesRepresentativeId create(SalesRepresentativeDetails details);

    void update(SalesRepresentativeId id, long expectedVersion, SalesRepresentativeDetails details);

    /** Historyczne zestawienia tracą powiązanie z opiekunem (ON DELETE SET NULL); zwykle lepiej dezaktywować. */
    void delete(SalesRepresentativeId id);
}
