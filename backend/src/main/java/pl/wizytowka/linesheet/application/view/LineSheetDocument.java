package pl.wizytowka.linesheet.application.view;

import pl.wizytowka.contact.application.view.SalesRepresentativeContact;
import pl.wizytowka.linesheet.domain.LineSheetItem;
import pl.wizytowka.linesheet.domain.Recipient;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Dane potrzebne do złożenia dokumentu PDF i wiadomości e-mail.
 * @param salesRepresentative opiekun handlowy do kontaktu lub {@code null}, gdy nie przypisano
 */
public record LineSheetDocument(UUID publicId,
                                String referenceNumber,
                                CatalogLanguage language,
                                String currency,
                                Recipient recipient,
                                String message,
                                Instant createdAt,
                                List<LineSheetItem> items,
                                SalesRepresentativeContact salesRepresentative) {

    public LineSheetDocument {
        items = List.copyOf(items);
    }
}
