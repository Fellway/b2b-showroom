package pl.wizytowka.linesheet.application.service;

import pl.wizytowka.contact.application.view.SalesRepresentativeContact;
import pl.wizytowka.linesheet.application.view.LineSheetDocument;
import pl.wizytowka.linesheet.domain.LineSheet;

final class LineSheetDocuments {

    private LineSheetDocuments() {
    }

    static LineSheetDocument of(LineSheet sheet, SalesRepresentativeContact salesRepresentative) {
        return new LineSheetDocument(sheet.publicId(), sheet.referenceNumber(), sheet.language(),
                sheet.currency().getCurrencyCode(), sheet.recipient(), sheet.message(), sheet.createdAt(),
                sheet.items(), salesRepresentative);
    }

    static String storageKey(LineSheet sheet) {
        int year = sheet.createdAt().atZone(java.time.ZoneOffset.UTC).getYear();
        return "line-sheets/%d/%s.pdf".formatted(year, sheet.publicId());
    }
}
