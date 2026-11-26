package pl.wizytowka.legal.application.view;

import pl.wizytowka.legal.domain.LegalDocumentType;

import java.util.UUID;

/**
 * Czego wymagamy od kupca w odniesieniu do jednego rodzaju dokumentu.
 *
 * @param currentVersionId          obowiązująca wersja (tę kupiec akceptuje / przyjmuje do wiadomości)
 * @param minimumVersionNumber      najniższa wersja, której akceptacja wystarcza — dla regulaminu najnowsza wersja
 *                                  wymagająca akceptacji (poprawki redakcyjne jej nie podnoszą), dla polityki
 *                                  prywatności zawsze obowiązująca
 * @param blocking                  {@code true} — bez akceptacji kupiec nie ma dostępu do cen i zestawień (regulamin);
 *                                  {@code false} — tylko informacja o zmianie (polityka prywatności)
 */
public record LegalRequirement(LegalDocumentType type,
                               UUID currentVersionId,
                               int currentVersionNumber,
                               int minimumVersionNumber,
                               boolean blocking) {
}
