package pl.wizytowka.linesheet.application.view;

import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.UUID;

/** @param buyerAccountId konto kupca; {@code null} dla zestawień historycznych i po usunięciu konta */
public record LineSheetSummaryView(UUID id,
                                   UUID publicId,
                                   String referenceNumber,
                                   CatalogLanguage language,
                                   String companyName,
                                   String contactName,
                                   String email,
                                   String phone,
                                   int itemCount,
                                   Instant createdAt,
                                   UUID buyerAccountId) {
}
