package pl.wizytowka.linesheet.application.port.in;

import java.util.UUID;

/**
 * Usunięcie danych osobowych kupca z jego zestawień po usunięciu konta (D25, art. 17 RODO). Zestawienie zostaje
 * jako ślad oferty (numer, data, pozycje, firma i numer podatkowy), ale bez danych osoby kontaktowej, wiadomości
 * i pliku PDF (który te dane zawiera).
 */
public interface ForgetBuyerUseCase {

    /** @return liczba zestawień, z których usunięto dane */
    int forgetBuyer(UUID buyerAccountId);
}
