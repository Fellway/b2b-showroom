package pl.wizytowka.linesheet.application.port.in;

import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @param salesRepresentativeId wybrany opiekun; {@code null} = pierwszy aktywny opiekun (jeśli istnieje)
 * @param visitorId             anonimowy identyfikator przeglądarki, przekazywany tylko po zgodzie na analitykę
 */
public record CreateLineSheetCommand(CatalogLanguage language,
                                     List<Selection> selections,
                                     String companyName,
                                     String contactName,
                                     String email,
                                     String phone,
                                     String taxId,
                                     String message,
                                     boolean privacyAccepted,
                                     boolean sendEmail,
                                     UUID salesRepresentativeId,
                                     UUID visitorId) {

    public CreateLineSheetCommand {
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
