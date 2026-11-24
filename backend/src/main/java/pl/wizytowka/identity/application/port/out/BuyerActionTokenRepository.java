package pl.wizytowka.identity.application.port.out;

import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.BuyerActionPurpose;
import pl.wizytowka.identity.domain.BuyerActionToken;

import java.time.Instant;
import java.util.Optional;

public interface BuyerActionTokenRepository {

    /** Odczyt z blokadą wiersza — token można wykorzystać tylko raz, także przy równoległych żądaniach. */
    Optional<BuyerActionToken> findByHashForUpdate(String tokenHash);

    void save(BuyerActionToken token);

    /** Unieważnia (oznacza jako użyte) wszystkie niewykorzystane tokeny danego rodzaju — przed wydaniem nowego. */
    void invalidateUnused(BuyerAccountId buyerAccountId, BuyerActionPurpose purpose, Instant now);

    /** Kiedy ostatnio wydano token danego rodzaju (ochrona przed zalewaniem skrzynki kupca). */
    Optional<Instant> lastIssuedAt(BuyerAccountId buyerAccountId, BuyerActionPurpose purpose);
}
