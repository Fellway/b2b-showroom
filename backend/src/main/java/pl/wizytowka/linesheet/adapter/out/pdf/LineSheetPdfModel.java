package pl.wizytowka.linesheet.adapter.out.pdf;

import java.util.List;

/**
 * Model szablonu PDF: wszystkie wartości są już sformatowane dla języka dokumentu (kwoty, daty, ilości),
 * dzięki czemu szablon wyłącznie wyświetla tekst i nie zawiera logiki.
 */
record LineSheetPdfModel(String languageTag,
                         Brand brand,
                         String referenceNumber,
                         String generatedAt,
                         List<Field> client,
                         Contact representative,
                         String message,
                         List<Row> rows,
                         Totals totals) {

    /** @param logoDataUri logo jako {@code data:} URI albo {@code null} (wtedy wordmark z nazwy) */
    record Brand(String name, String logoDataUri, String accentColor, List<String> footerParts) {
    }

    /** @param labelKey klucz etykiety w pakiecie komunikatów */
    record Field(String labelKey, String value) {
    }

    record Contact(String name, String email, String phone) {
    }

    /** @param thumbnail miniatura jako {@code data:} URI albo {@code null} (placeholder) */
    record Row(String thumbnail,
               String productName,
               String styleCode,
               String color,
               String size,
               String ean,
               String sku,
               String quantity,
               String wholesalePrice,
               String retailPrice) {
    }

    /**
     * @param totalQuantity  suma ilości lub {@code null}, gdy klient nie podał żadnej
     * @param wholesaleValue wartość hurtowa pozycji z ilością lub {@code null}
     * @param partial        część pozycji nie ma ilości — wartość dotyczy tylko pozycji z ilością
     */
    record Totals(int positions, String totalQuantity, String wholesaleValue, boolean partial) {
    }
}
