package pl.wizytowka.contact.adapter.out.persistence;

import org.springframework.stereotype.Component;
import pl.wizytowka.contact.application.port.out.SalesRepresentativeRepository;
import pl.wizytowka.contact.domain.SalesRepresentative;
import pl.wizytowka.contact.domain.SalesRepresentativeDetails;
import pl.wizytowka.contact.domain.SalesRepresentativeId;
import pl.wizytowka.shared.application.Versions;

import java.util.List;
import java.util.Optional;

@Component
class SalesRepresentativePersistenceAdapter implements SalesRepresentativeRepository {

    private final SalesRepresentativeJpaRepository repository;

    SalesRepresentativePersistenceAdapter(SalesRepresentativeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SalesRepresentative> findById(SalesRepresentativeId id) {
        return repository.findById(id.value()).map(SalesRepresentativePersistenceAdapter::toDomain);
    }

    @Override
    public List<SalesRepresentative> findAll() {
        return repository.findAllByOrderBySortOrderAscFullNameAsc().stream()
                .map(SalesRepresentativePersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public void save(SalesRepresentative representative) {
        SalesRepresentativeJpaEntity entity = repository.findById(representative.id().value())
                .orElseGet(() -> new SalesRepresentativeJpaEntity(representative.id().value()));
        if (entity.getVersion() != null) {
            Versions.require(representative.version(), entity.getVersion());
        }
        SalesRepresentativeDetails details = representative.details();
        entity.setFullName(details.fullName());
        entity.setEmail(details.email());
        entity.setPhone(details.phone());
        entity.setActive(details.active());
        entity.setSortOrder(details.sortOrder());
        repository.save(entity);
    }

    @Override
    public void delete(SalesRepresentative representative) {
        repository.deleteById(representative.id().value());
    }

    @Override
    public boolean existsByEmail(String email, SalesRepresentativeId excluded) {
        return excluded == null
                ? repository.existsByEmail(email)
                : repository.existsByEmailAndIdNot(email, excluded.value());
    }

    private static SalesRepresentative toDomain(SalesRepresentativeJpaEntity entity) {
        return SalesRepresentative.restore(
                SalesRepresentativeId.of(entity.getId()),
                new SalesRepresentativeDetails(entity.getFullName(), entity.getEmail(), entity.getPhone(),
                        entity.isActive(), entity.getSortOrder()),
                entity.getVersion());
    }
}
