package pl.wizytowka.identity.application.port.out;

import pl.wizytowka.identity.domain.AdminUserId;
import pl.wizytowka.identity.domain.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    /** Odczyt z blokadą wiersza — współbieżne odświeżenia tym samym tokenem są serializowane. */
    Optional<RefreshToken> findByHashForUpdate(String tokenHash);

    void save(RefreshToken token);

    void revokeFamily(UUID familyId, Instant now);

    void revokeAllOfUser(AdminUserId adminUserId, Instant now);
}
