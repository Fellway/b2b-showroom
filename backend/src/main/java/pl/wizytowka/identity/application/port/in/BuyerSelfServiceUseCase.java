package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.shared.domain.CatalogLanguage;

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
}
