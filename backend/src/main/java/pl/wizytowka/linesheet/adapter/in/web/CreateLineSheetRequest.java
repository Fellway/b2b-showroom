package pl.wizytowka.linesheet.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pl.wizytowka.linesheet.application.port.in.CreateLineSheetCommand;
import pl.wizytowka.linesheet.domain.LineSheet;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.List;
import java.util.UUID;

/**
 * @param privacyAccepted zgoda na przetwarzanie danych — wymagana, gdy podano dane kontaktowe lub zlecono wysyłkę
 * @param visitorId       anonimowe id przeglądarki; frontend przekazuje je tylko po zgodzie na analitykę
 */
record CreateLineSheetRequest(@NotEmpty @Size(max = LineSheet.MAX_ITEMS) List<@Valid @NotNull Item> items,
                              @Size(max = 200) String companyName,
                              @Size(max = 120) String contactName,
                              @Email @Size(max = 254) String email,
                              @Size(max = 32) String phone,
                              @Size(max = 32) String taxId,
                              @Size(max = 2000) String message,
                              boolean privacyAccepted,
                              boolean sendEmail,
                              UUID salesRepresentativeId,
                              UUID visitorId) {

    record Item(@NotNull UUID variantId, @Positive Integer quantity) {
    }

    CreateLineSheetCommand toCommand(CatalogLanguage language) {
        return new CreateLineSheetCommand(language,
                items.stream().map(i -> new CreateLineSheetCommand.Selection(i.variantId(), i.quantity())).toList(),
                companyName, contactName, email, phone, taxId, message, privacyAccepted, sendEmail,
                salesRepresentativeId, visitorId);
    }
}
