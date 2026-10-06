package pl.wizytowka.contact.application.service;

import org.junit.jupiter.api.Test;
import pl.wizytowka.contact.application.port.out.SalesRepresentativeRepository;
import pl.wizytowka.contact.application.view.SalesRepresentativeContact;
import pl.wizytowka.contact.domain.SalesRepresentative;
import pl.wizytowka.contact.domain.SalesRepresentativeDetails;
import pl.wizytowka.contact.domain.SalesRepresentativeId;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.shared.domain.ResourceConflictException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalesRepresentativeServiceTest {

    private final InMemoryRepository repository = new InMemoryRepository();
    private final SalesRepresentativeService service = new SalesRepresentativeService(repository);

    @Test
    void listsOnlyActiveRepresentativesInOrderForPublic() {
        service.create(details("Zofia", "z@brand.pl", true, 2));
        service.create(details("Adam", "a@brand.pl", true, 1));
        service.create(details("Ewa", "e@brand.pl", false, 0));

        assertThat(service.activeRepresentatives()).extracting(SalesRepresentativeContact::fullName)
                .containsExactly("Adam", "Zofia");
        assertThat(service.allRepresentatives()).hasSize(3);
    }

    @Test
    void rejectsDuplicateEmail() {
        service.create(details("Adam", "a@brand.pl", true, 0));

        assertThatThrownBy(() -> service.create(details("Adam 2", "A@Brand.pl", true, 0)))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void updateRequiresCurrentVersionAndAllowsKeepingOwnEmail() {
        SalesRepresentativeId id = service.create(details("Adam", "a@brand.pl", true, 0));

        service.update(id, 0, details("Adam Kowalski", "a@brand.pl", true, 0));

        assertThat(service.representative(id).fullName()).isEqualTo("Adam Kowalski");
        assertThatThrownBy(() -> service.update(id, 7, details("X", "a@brand.pl", true, 0)))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void findActiveContactIgnoresInactive() {
        SalesRepresentativeId id = service.create(details("Ewa", "e@brand.pl", false, 0));

        assertThat(service.findActiveContact(id.value())).isEmpty();
        assertThat(service.findContact(id.value())).isPresent();
        assertThatThrownBy(() -> service.delete(SalesRepresentativeId.newId())).isInstanceOf(ResourceNotFoundException.class);
    }

    private static SalesRepresentativeDetails details(String name, String email, boolean active, int sortOrder) {
        return new SalesRepresentativeDetails(name, email, null, active, sortOrder);
    }

    static final class InMemoryRepository implements SalesRepresentativeRepository {

        private final Map<SalesRepresentativeId, SalesRepresentative> store = new LinkedHashMap<>();

        @Override
        public Optional<SalesRepresentative> findById(SalesRepresentativeId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<SalesRepresentative> findAll() {
            return store.values().stream()
                    .sorted(Comparator.comparingInt((SalesRepresentative r) -> r.details().sortOrder())
                            .thenComparing(r -> r.details().fullName()))
                    .toList();
        }

        @Override
        public void save(SalesRepresentative representative) {
            store.put(representative.id(), representative);
        }

        @Override
        public void delete(SalesRepresentative representative) {
            store.remove(representative.id());
        }

        @Override
        public boolean existsByEmail(String email, SalesRepresentativeId excluded) {
            return store.values().stream()
                    .anyMatch(r -> r.details().email().equals(email) && !r.id().equals(excluded));
        }
    }
}
