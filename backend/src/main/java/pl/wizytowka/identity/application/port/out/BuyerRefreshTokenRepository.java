package pl.wizytowka.identity.application.port.out;

import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Sesje kupców — osobny magazyn niż sesje panelu ({@link RefreshTokenRepository}). */
public interface BuyerRefreshTokenRepository {

    /** Odczyt z blokadą wiersza — współbieżne odświeżenia tym samym tokenem są serializowane. */
    Optional<RefreshToken> findByHashForUpdate(String tokenHash);

    void save(RefreshToken token);

    void revokeFamily(UUID familyId, Instant now);

    void revokeAllOfBuyer(BuyerAccountId buyerAccountId, Instant now);
}
