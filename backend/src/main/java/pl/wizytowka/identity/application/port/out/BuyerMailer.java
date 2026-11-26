package pl.wizytowka.identity.application.port.out;

import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.List;

/**
 * E-maile transakcyjne kont kupców (D23). Każda metoda odpowiada stałemu szablonowi; do treści trafiają wyłącznie
 * linki z tokenem wygenerowane przez aplikację — nigdy dane wpisane przez użytkownika (nazwa firmy, notatki),
 * więc publiczny formularz nie może posłużyć do wysyłania spamu z domeny marki.
 *
 * <p>Wysyłka jest asynchroniczna i nie zgłasza błędów do wywołującego (czas odpowiedzi API nie może zdradzać,
 * czy konto istnieje, a awaria serwera poczty nie może cofać rejestracji).</p>
 *
 * @see pl.wizytowka.identity.application.port.in.BuyerRegistrationUseCase
 */
public interface BuyerMailer {

    void sendEmailVerification(String to, CatalogLanguage language, String rawToken);

    /** Ktoś próbował zarejestrować konto na istniejący adres — informujemy właściciela zamiast ujawniać to w API. */
    void sendAccountAlreadyExists(String to, CatalogLanguage language);

    void sendAccountApproved(String to, CatalogLanguage language);

    void sendAccountRejected(String to, CatalogLanguage language);

    void sendPasswordReset(String to, CatalogLanguage language, String rawToken);

    /** Potwierdzenie, że kupiec usunął konto (D25) — bez linków z tokenem. */
    void sendAccountDeleted(String to, CatalogLanguage language);

    /** Powiadomienie zespołu marki o nowym wniosku czekającym na akceptację (bez danych kupca w treści). */
    void notifyStaffAboutPendingBuyer(List<String> staffEmails);
}
