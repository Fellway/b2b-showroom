package pl.wizytowka.identity.application.port.in;

/**
 * Publiczne operacje bez logowania: rejestracja, potwierdzenie adresu, reset hasła.
 *
 * <p>Odpowiedzi nie zdradzają, czy konto o danym adresie istnieje: rejestracja na zajęty adres, ponowna wysyłka
 * potwierdzenia i prośba o reset hasła kończą się zawsze tak samo (informacja trafia e-mailem do właściciela adresu).</p>
 */
public interface BuyerRegistrationUseCase {

    /**
     * Zakłada konto w stanie oczekiwania na potwierdzenie adresu i wysyła link potwierdzający.
     * @throws pl.wizytowka.shared.domain.InvalidValueException przy niepoprawnych danych, haśle lub braku zgód
     */
    void register(RegisterBuyerCommand command);

    /**
     * Potwierdza adres e-mail tokenem z wiadomości; konto przechodzi do oczekiwania na akceptację marki.
     * @throws pl.wizytowka.shared.domain.InvalidValueException {@code identity.buyer.token-invalid} — token nieznany, zużyty lub wygasły
     */
    void verifyEmail(String rawToken);

    /** Ponownie wysyła link potwierdzający, jeśli konto czeka na potwierdzenie adresu (w przeciwnym razie nic nie robi). */
    void resendVerification(String email);

    /** Wysyła link do ustawienia nowego hasła, jeśli konto istnieje (w przeciwnym razie nic nie robi). */
    void requestPasswordReset(String email);

    /**
     * Ustawia nowe hasło tokenem z wiadomości i kończy wszystkie sesje kupca.
     * @throws pl.wizytowka.shared.domain.InvalidValueException token niepoprawny albo hasło niezgodne z polityką
     */
    void resetPassword(String rawToken, String newPassword);
}
