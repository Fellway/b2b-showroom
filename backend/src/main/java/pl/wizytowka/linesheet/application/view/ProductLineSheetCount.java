package pl.wizytowka.linesheet.application.view;

import java.util.UUID;

/** @param lineSheetCount w ilu zestawieniach produkt wystąpił (niezależnie od liczby wariantów) */
public record ProductLineSheetCount(UUID productId, long lineSheetCount) {
}
