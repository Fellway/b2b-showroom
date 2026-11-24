package pl.wizytowka.identity.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import pl.wizytowka.identity.application.port.out.RefreshTokenRepository;
import pl.wizytowka.identity.domain.AdminUserId;
import pl.wizytowka.identity.domain.RefreshToken;
import pl.wizytowka.identity.domain.RefreshTokenId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository repository;
    private final EntityManager entityManager;

    RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(RefreshTokenPersistenceAdapter::toDomain);
    }

    @Override
    public void save(RefreshToken token) {
        Optional<RefreshTokenJpaEntity> existing = repository.findById(token.id().value());
        if (existing.isPresent()) {
            existing.get().setUsedAt(token.usedAt());
            existing.get().setRevokedAt(token.revokedAt());
            return;
        }
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity(token.id().value(), token.subjectId(),
                token.familyId(), token.tokenHash(), token.issuedAt(), token.expiresAt());
        entity.setUsedAt(token.usedAt());
        entity.setRevokedAt(token.revokedAt());
        // przypisany identyfikator: persist zamiast merge, bez zbędnego SELECT-a
        entityManager.persist(entity);
    }

    @Override
    public void revokeFamily(UUID familyId, Instant now) {
        repository.revokeFamily(familyId, now);
    }

    @Override
    public void revokeAllOfUser(AdminUserId adminUserId, Instant now) {
        repository.revokeAllOfUser(adminUserId.value(), now);
    }

    private static RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return RefreshToken.restore(RefreshTokenId.of(e.getId()), e.getAdminUserId(), e.getFamilyId(),
                e.getTokenHash(), e.getIssuedAt(), e.getExpiresAt(), e.getUsedAt(), e.getRevokedAt());
    }
}
