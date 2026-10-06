package pl.wizytowka.linesheet.application.port.in;

import pl.wizytowka.linesheet.application.view.LineSheetPdf;

import java.util.UUID;

public interface GetLineSheetPdfUseCase {

    /** @throws pl.wizytowka.shared.domain.ResourceNotFoundException gdy zestawienie lub plik nie istnieje */
    LineSheetPdf pdf(UUID publicId);
}
