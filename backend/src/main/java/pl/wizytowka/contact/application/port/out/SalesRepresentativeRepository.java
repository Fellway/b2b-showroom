package pl.wizytowka.contact.application.port.out;

import pl.wizytowka.contact.domain.SalesRepresentative;
import pl.wizytowka.contact.domain.SalesRepresentativeId;

import java.util.List;
import java.util.Optional;

public interface SalesRepresentativeRepository {

    Optional<SalesRepresentative> findById(SalesRepresentativeId id);

    /** Wszyscy opiekunowie posortowani po sortOrder, potem po nazwisku. */
    List<SalesRepresentative> findAll();

    /** @throws pl.wizytowka.shared.domain.ConcurrentModificationException przy niezgodności wersji */
    void save(SalesRepresentative representative);

    void delete(SalesRepresentative representative);

    boolean existsByEmail(String email, SalesRepresentativeId excluded);
}
