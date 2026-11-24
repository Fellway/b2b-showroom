package pl.wizytowka.linesheet.application.port.in;

import pl.wizytowka.linesheet.application.view.LineSheetPdf;
import pl.wizytowka.linesheet.domain.LineSheetId;

import java.util.UUID;

/** Pobranie PDF zestawienia — zawiera ceny i dane klienta, więc nigdy bez uwierzytelnienia (D23). */
public interface GetLineSheetPdfUseCase {

    /**
     * PDF dla właściciela zestawienia. Cudze zestawienie jest nieodróżnialne od nieistniejącego.
     * @throws pl.wizytowka.shared.domain.ResourceNotFoundException gdy zestawienie nie istnieje, należy do kogoś innego
     *                                                              lub plik nie istnieje
     */
    LineSheetPdf pdfForBuyer(UUID buyerAccountId, UUID publicId);

    /** @throws pl.wizytowka.shared.domain.ResourceNotFoundException gdy zestawienie lub plik nie istnieje */
    LineSheetPdf pdfForStaff(LineSheetId id);
}
