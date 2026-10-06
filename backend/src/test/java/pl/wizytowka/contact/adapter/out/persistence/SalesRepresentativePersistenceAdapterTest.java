package pl.wizytowka.contact.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import pl.wizytowka.contact.application.port.out.SalesRepresentativeRepository;
import pl.wizytowka.contact.domain.SalesRepresentative;
import pl.wizytowka.contact.domain.SalesRepresentativeDetails;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.support.PersistenceTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PersistenceTest
class SalesRepresentativePersistenceAdapterTest {

    @Autowired
    SalesRepresentativeRepository repository;

    @Autowired
    TestEntityManager em;

    @Test
    void savesUpdatesAndChecksVersion() {
        SalesRepresentative created = SalesRepresentative.create(
                new SalesRepresentativeDetails("Anna Nowak", "anna@brand.pl", "+48 600 100 200", true, 1));
        repository.save(created);
        em.flush();
        em.clear();

        SalesRepresentative loaded = repository.findById(created.id()).orElseThrow();
        assertThat(loaded.details().phone()).isEqualTo("+48 600 100 200");
        assertThat(loaded.version()).isZero();
        assertThat(repository.existsByEmail("anna@brand.pl", null)).isTrue();
        assertThat(repository.existsByEmail("anna@brand.pl", created.id())).isFalse();

        loaded.update(new SalesRepresentativeDetails("Anna Kowalska", "anna@brand.pl", null, false, 1));
        repository.save(loaded);
        em.flush();
        em.clear();

        assertThat(repository.findById(created.id()).orElseThrow().version()).isEqualTo(1);
        assertThat(repository.findAll()).singleElement()
                .satisfies(r -> assertThat(r.details().fullName()).isEqualTo("Anna Kowalska"));
        assertThatThrownBy(() -> repository.save(loaded)).as("zapis nieaktualnej wersji")
                .isInstanceOf(ConcurrentModificationException.class);
    }
}
