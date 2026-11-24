package pl.wizytowka.linesheet.application.port.in;

import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Zestawienie generuje zalogowany kupiec (D23); dane kontaktowe w PDF pochodzą z jego konta (migawka),
 * a zgody na przetwarzanie danych kupiec złożył przy rejestracji.
 *
 * @param buyerAccountId        konto kupca z tokenu dostępu
 * @param salesRepresentativeId wybrany opiekun; {@code null} = pierwszy aktywny opiekun (jeśli istnieje)
 * @param visitorId             anonimowy identyfikator przeglądarki, przekazywany tylko po zgodzie na analitykę
 */
public record CreateLineSheetCommand(UUID buyerAccountId,
                                     CatalogLanguage language,
                                     List<Selection> selections,
                                     String message,
                                     UUID salesRepresentativeId,
                                     UUID visitorId) {

    public CreateLineSheetCommand {
        Objects.requireNonNull(buyerAccountId, "buyerAccountId");
        Objects.requireNonNull(language, "language");
        selections = selections == null ? List.of() : List.copyOf(selections);
    }

    /** @param quantity opcjonalna ilość orientacyjna */
    public record Selection(UUID variantId, Integer quantity) {

        public Selection {
            Objects.requireNonNull(variantId, "variantId");
        }
    }
}
