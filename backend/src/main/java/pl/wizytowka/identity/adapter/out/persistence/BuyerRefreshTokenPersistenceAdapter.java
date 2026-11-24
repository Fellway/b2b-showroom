package pl.wizytowka.identity.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import pl.wizytowka.identity.application.port.out.BuyerRefreshTokenRepository;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.RefreshToken;
import pl.wizytowka.identity.domain.RefreshTokenId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
class BuyerRefreshTokenPersistenceAdapter implements BuyerRefreshTokenRepository {

    private final BuyerRefreshTokenJpaRepository repository;
    private final EntityManager entityManager;

    BuyerRefreshTokenPersistenceAdapter(BuyerRefreshTokenJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(BuyerRefreshTokenPersistenceAdapter::toDomain);
    }

    @Override
    public void save(RefreshToken token) {
        Optional<BuyerRefreshTokenJpaEntity> existing = repository.findById(token.id().value());
        if (existing.isPresent()) {
            existing.get().usedAt = token.usedAt();
            existing.get().revokedAt = token.revokedAt();
            return;
        }
        BuyerRefreshTokenJpaEntity entity = new BuyerRefreshTokenJpaEntity(token.id().value(), token.subjectId(),
                token.familyId(), token.tokenHash(), token.issuedAt(), token.expiresAt());
        entity.usedAt = token.usedAt();
        entity.revokedAt = token.revokedAt();
        // przypisany identyfikator: persist zamiast merge, bez zbędnego SELECT-a
        entityManager.persist(entity);
    }

    @Override
    public void revokeFamily(UUID familyId, Instant now) {
        repository.revokeFamily(familyId, now);
    }

    @Override
    public void revokeAllOfBuyer(BuyerAccountId buyerAccountId, Instant now) {
        repository.revokeAllOfBuyer(buyerAccountId.value(), now);
    }

    private static RefreshToken toDomain(BuyerRefreshTokenJpaEntity e) {
        return RefreshToken.restore(RefreshTokenId.of(e.id), e.buyerAccountId, e.familyId, e.tokenHash, e.issuedAt,
                e.expiresAt, e.usedAt, e.revokedAt);
    }
}
