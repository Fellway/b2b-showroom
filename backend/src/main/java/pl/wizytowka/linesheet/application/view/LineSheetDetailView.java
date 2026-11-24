package pl.wizytowka.linesheet.application.view;

import pl.wizytowka.contact.application.view.SalesRepresentativeContact;
import pl.wizytowka.linesheet.domain.LineSheetItem;
import pl.wizytowka.linesheet.domain.Recipient;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param wholesaleTotal suma wartości hurtowych pozycji z podaną ilością; {@code null}, gdy żadna nie ma ilości
 * @param salesRepresentative {@code null}, gdy nie przypisano lub opiekun został usunięty
 */
public record LineSheetDetailView(UUID id,
                                  UUID publicId,
                                  String referenceNumber,
                                  CatalogLanguage language,
                                  String currency,
                                  Recipient recipient,
                                  String message,
                                  Instant privacyAcceptedAt,
                                  SalesRepresentativeContact salesRepresentative,
                                  boolean pdfAvailable,
                                  Instant pdfGeneratedAt,
                                  Instant createdAt,
                                  List<LineSheetItem> items,
                                  BigDecimal wholesaleTotal,
                                  UUID buyerAccountId,
                                  long version) {
}
