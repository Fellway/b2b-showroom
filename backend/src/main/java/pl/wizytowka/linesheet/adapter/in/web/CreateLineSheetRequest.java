package pl.wizytowka.linesheet.adapter.in.web;

import jakarta.validation.Valid;
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
 * Dane kontaktowe nie są przesyłane — PDF zawiera dane konta kupca (D23).
 *
 * @param visitorId anonimowe id przeglądarki; frontend przekazuje je tylko po zgodzie na analitykę
 */
record CreateLineSheetRequest(@NotEmpty @Size(max = LineSheet.MAX_ITEMS) List<@Valid @NotNull Item> items,
                              @Size(max = 2000) String message,
                              UUID salesRepresentativeId,
                              UUID visitorId) {

    record Item(@NotNull UUID variantId, @Positive Integer quantity) {
    }

    CreateLineSheetCommand toCommand(UUID buyerAccountId, CatalogLanguage language) {
        return new CreateLineSheetCommand(buyerAccountId, language,
                items.stream().map(i -> new CreateLineSheetCommand.Selection(i.variantId(), i.quantity())).toList(),
                message, salesRepresentativeId, visitorId);
    }
}
