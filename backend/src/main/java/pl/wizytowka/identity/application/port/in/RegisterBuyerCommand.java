package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.Objects;
import java.util.UUID;

/**
 * Dane z formularza rejestracji kupca. Hasło nigdy nie trafia do {@link #toString()}.
 *
 * @param termsVersionId   wersja regulaminu pokazana w formularzu (D25)
 * @param privacyVersionId wersja polityki prywatności pokazana w formularzu
 */
public record RegisterBuyerCommand(String email,
                                   String password,
                                   String companyName,
                                   String countryCode,
                                   String taxId,
                                   String contactName,
                                   String phone,
                                   CatalogLanguage language,
                                   boolean termsAccepted,
                                   boolean privacyAccepted,
                                   UUID termsVersionId,
                                   UUID privacyVersionId) {

    public RegisterBuyerCommand {
        Objects.requireNonNull(language, "language");
    }

    @Override
    public String toString() {
        return "RegisterBuyerCommand[email=" + email + ", country=" + countryCode + "]";
    }
}
