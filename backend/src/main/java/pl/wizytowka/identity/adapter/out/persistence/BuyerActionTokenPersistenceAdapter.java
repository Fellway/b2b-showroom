package pl.wizytowka.identity.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import pl.wizytowka.identity.application.port.out.BuyerActionTokenRepository;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.BuyerActionPurpose;
import pl.wizytowka.identity.domain.BuyerActionToken;

import java.time.Instant;
import java.util.Optional;

@Component
class BuyerActionTokenPersistenceAdapter implements BuyerActionTokenRepository {

    private final BuyerActionTokenJpaRepository repository;
    private final EntityManager entityManager;

    BuyerActionTokenPersistenceAdapter(BuyerActionTokenJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<BuyerActionToken> findByHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(BuyerActionTokenPersistenceAdapter::toDomain);
    }

    @Override
    public void save(BuyerActionToken token) {
        Optional<BuyerActionTokenJpaEntity> existing = repository.findById(token.id());
        if (existing.isPresent()) {
            existing.get().usedAt = token.usedAt();
            return;
        }
        BuyerActionTokenJpaEntity entity = new BuyerActionTokenJpaEntity(token.id());
        entity.buyerAccountId = token.buyerAccountId().value();
        entity.purpose = token.purpose();
        entity.tokenHash = token.tokenHash();
        entity.createdAt = token.createdAt();
        entity.expiresAt = token.expiresAt();
        entity.usedAt = token.usedAt();
        entityManager.persist(entity);
    }

    @Override
    public void invalidateUnused(BuyerAccountId buyerAccountId, BuyerActionPurpose purpose, Instant now) {
        repository.invalidateUnused(buyerAccountId.value(), purpose, now);
    }

    @Override
    public Optional<Instant> lastIssuedAt(BuyerAccountId buyerAccountId, BuyerActionPurpose purpose) {
        return repository.lastIssuedAt(buyerAccountId.value(), purpose);
    }

    private static BuyerActionToken toDomain(BuyerActionTokenJpaEntity e) {
        return BuyerActionToken.restore(e.id, BuyerAccountId.of(e.buyerAccountId), e.purpose, e.tokenHash, e.createdAt,
                e.expiresAt, e.usedAt);
    }
}
