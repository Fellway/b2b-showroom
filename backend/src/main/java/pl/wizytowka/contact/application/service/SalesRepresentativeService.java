package pl.wizytowka.contact.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.contact.application.port.in.ManageSalesRepresentativesUseCase;
import pl.wizytowka.contact.application.port.in.SalesRepresentativeQuery;
import pl.wizytowka.contact.application.port.out.SalesRepresentativeRepository;
import pl.wizytowka.contact.application.view.AdminSalesRepresentativeView;
import pl.wizytowka.contact.application.view.SalesRepresentativeContact;
import pl.wizytowka.contact.domain.SalesRepresentative;
import pl.wizytowka.contact.domain.SalesRepresentativeDetails;
import pl.wizytowka.contact.domain.SalesRepresentativeId;
import pl.wizytowka.shared.application.Versions;
import pl.wizytowka.shared.domain.ResourceConflictException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
class SalesRepresentativeService implements ManageSalesRepresentativesUseCase, SalesRepresentativeQuery {

    private final SalesRepresentativeRepository representatives;

    SalesRepresentativeService(SalesRepresentativeRepository representatives) {
        this.representatives = representatives;
    }

    @Override
    public SalesRepresentativeId create(SalesRepresentativeDetails details) {
        requireUniqueEmail(details.email(), null);
        SalesRepresentative representative = SalesRepresentative.create(details);
        representatives.save(representative);
        return representative.id();
    }

    @Override
    public void update(SalesRepresentativeId id, long expectedVersion, SalesRepresentativeDetails details) {
        SalesRepresentative representative = load(id);
        Versions.require(expectedVersion, representative.version());
        requireUniqueEmail(details.email(), id);
        representative.update(details);
        representatives.save(representative);
    }

    @Override
    public void delete(SalesRepresentativeId id) {
        representatives.delete(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesRepresentativeContact> activeRepresentatives() {
        return representatives.findAll().stream()
                .filter(SalesRepresentative::isActive)
                .map(SalesRepresentativeService::toContact)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminSalesRepresentativeView> allRepresentatives() {
        return representatives.findAll().stream().map(SalesRepresentativeService::toAdminView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSalesRepresentativeView representative(SalesRepresentativeId id) {
        return toAdminView(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SalesRepresentativeContact> findContact(UUID id) {
        return representatives.findById(SalesRepresentativeId.of(id)).map(SalesRepresentativeService::toContact);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SalesRepresentativeContact> findActiveContact(UUID id) {
        return representatives.findById(SalesRepresentativeId.of(id))
                .filter(SalesRepresentative::isActive)
                .map(SalesRepresentativeService::toContact);
    }

    private SalesRepresentative load(SalesRepresentativeId id) {
        return representatives.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("contact.representative.not-found",
                        "Sales representative not found"));
    }

    private void requireUniqueEmail(String email, SalesRepresentativeId excluded) {
        if (representatives.existsByEmail(email, excluded)) {
            throw new ResourceConflictException("contact.representative.email-taken",
                    "Another sales representative already uses this e-mail");
        }
    }

    private static SalesRepresentativeContact toContact(SalesRepresentative representative) {
        SalesRepresentativeDetails d = representative.details();
        return new SalesRepresentativeContact(representative.id().value(), d.fullName(), d.email(), d.phone());
    }

    private static AdminSalesRepresentativeView toAdminView(SalesRepresentative representative) {
        SalesRepresentativeDetails d = representative.details();
        return new AdminSalesRepresentativeView(representative.id().value(), d.fullName(), d.email(), d.phone(),
                d.active(), d.sortOrder(), representative.version());
    }
}
