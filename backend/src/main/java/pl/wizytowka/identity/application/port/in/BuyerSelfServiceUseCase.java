package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.List;

/** Operacje zalogowanego kupca na własnym koncie. */
public interface BuyerSelfServiceUseCase {

    BuyerAccountView me(BuyerAccountId id);

    /** Kupiec zmienia osobę kontaktową, telefon i język; firmę i numer podatkowy zmienia marka. */
    void updateProfile(BuyerAccountId id, long expectedVersion, String contactName, String phone, CatalogLanguage language);

    /**
     * Zmienia hasło, kończy wszystkie dotychczasowe sesje i otwiera nową (kupiec zostaje zalogowany).
     * @throws pl.wizytowka.shared.domain.InvalidValueException {@code identity.buyer.current-password-invalid}
     *                                                          albo hasło niezgodne z polityką
     */
    BuyerSession changePassword(BuyerAccountId id, String currentPassword, String newPassword);

    /** Dane konta i historia zgód do eksportu (prawo dostępu i przenoszenia danych, art. 15 i 20 RODO). */
    BuyerDataExport export(BuyerAccountId id);

    /**
     * Kupiec usuwa własne konto (art. 17 RODO): sesje, tokeny i rejestr zgód znikają razem z kontem, a z jego
     * zestawień usuwane są dane osobowe ({@link pl.wizytowka.identity.domain.BuyerAccountDeleted}).
     * Wymaga bieżącego hasła — ta sama ochrona przed zgadywaniem co przy logowaniu.
     *
     * @throws pl.wizytowka.shared.domain.InvalidValueException {@code identity.buyer.current-password-invalid}
     */
    void deleteAccount(BuyerAccountId id, String currentPassword);

    /**
     * @param emailVerifiedAt kiedy kupiec potwierdził adres e-mail
     * @param lastLoginAt     ostatnie udane logowanie
     */
    record BuyerDataExport(BuyerAccountView account,
                           Instant emailVerifiedAt,
                           Instant reviewedAt,
                           Instant lastLoginAt,
                           List<BuyerConsentView> consents) {
    }
}
