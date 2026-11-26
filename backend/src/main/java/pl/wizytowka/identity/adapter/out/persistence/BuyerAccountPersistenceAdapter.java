package pl.wizytowka.identity.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import pl.wizytowka.identity.application.port.in.BuyerFilter;
import pl.wizytowka.identity.application.port.out.BuyerAccountRepository;
import pl.wizytowka.identity.domain.AdminUserId;
import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.BuyerStatus;
import pl.wizytowka.identity.domain.TaxId;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.ConcurrentModificationException;

import java.util.Locale;
import java.util.Optional;

@Component
class BuyerAccountPersistenceAdapter implements BuyerAccountRepository {

    private final BuyerAccountJpaRepository repository;

    BuyerAccountPersistenceAdapter(BuyerAccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<BuyerAccount> findById(BuyerAccountId id) {
        return repository.findById(id.value()).map(BuyerAccountPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<BuyerAccount> findByEmail(String normalizedEmail) {
        return repository.findByEmail(normalizedEmail).map(BuyerAccountPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsByEmail(String normalizedEmail) {
        return repository.existsByEmail(normalizedEmail);
    }

    @Override
    public void save(BuyerAccount account) {
        BuyerAccountJpaEntity entity = repository.findById(account.id().value())
                .map(existing -> {
                    if (existing.version != account.version()) {
                        throw new ConcurrentModificationException("identity.buyer.concurrent-modification",
                                "Buyer account was modified by someone else");
                    }
                    return existing;
                })
                .orElseGet(() -> newEntity(account));
        entity.passwordHash = account.passwordHash();
        entity.companyName = account.companyName();
        entity.countryCode = account.taxId().countryCode();
        entity.taxId = account.taxId().number();
        entity.contactName = account.contactName();
        entity.phone = account.phone();
        entity.language = account.language();
        entity.status = account.status();
        entity.emailVerifiedAt = account.emailVerifiedAt();
        entity.reviewedAt = account.reviewedAt();
        entity.reviewedBy = account.reviewedBy() == null ? null : account.reviewedBy().value();
        entity.reviewNote = account.reviewNote();
        entity.failedLoginAttempts = account.failedLoginAttempts();
        entity.lockedUntil = account.lockedUntil();
        entity.lastLoginAt = account.lastLoginAt();
        repository.save(entity);
    }

    @Override
    public void delete(BuyerAccountId id) {
        repository.deleteById(id.value());
    }

    @Override
    public PageResult<BuyerAccount> find(BuyerFilter filter, PageQuery page) {
        String pattern = filter.search() == null ? null : containsPattern(filter.search());
        Page<BuyerAccountJpaEntity> result = repository.find(filter.status(), pattern,
                PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent().stream().map(BuyerAccountPersistenceAdapter::toDomain).toList(),
                page.page(), page.size(), result.getTotalElements());
    }

    @Override
    public long countByStatus(BuyerStatus status) {
        return repository.countByStatus(status);
    }

    private static BuyerAccountJpaEntity newEntity(BuyerAccount account) {
        BuyerAccountJpaEntity entity = new BuyerAccountJpaEntity(account.id().value());
        entity.email = account.email();
        entity.createdAt = account.createdAt();
        return entity;
    }

    /** Wzorzec LIKE „zawiera” z escapowaniem znaków specjalnych (znak ucieczki: '!'). */
    static String containsPattern(String search) {
        String escaped = search.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    private static BuyerAccount toDomain(BuyerAccountJpaEntity e) {
        return BuyerAccount.restore()
                .id(BuyerAccountId.of(e.id))
                .email(e.email)
                .passwordHash(e.passwordHash)
                .companyName(e.companyName)
                .taxId(TaxId.of(e.countryCode, e.taxId))
                .contactName(e.contactName)
                .phone(e.phone)
                .language(e.language)
                .status(e.status)
                .emailVerifiedAt(e.emailVerifiedAt)
                .reviewedAt(e.reviewedAt)
                .reviewedBy(e.reviewedBy == null ? null : AdminUserId.of(e.reviewedBy))
                .reviewNote(e.reviewNote)
                .createdAt(e.createdAt)
                .failedLoginAttempts(e.failedLoginAttempts)
                .lockedUntil(e.lockedUntil)
                .lastLoginAt(e.lastLoginAt)
                .version(e.version)
                .build();
    }
}
