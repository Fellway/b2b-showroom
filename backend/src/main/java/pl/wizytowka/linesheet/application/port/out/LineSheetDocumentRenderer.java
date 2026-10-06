package pl.wizytowka.linesheet.application.port.out;

import pl.wizytowka.linesheet.application.view.LineSheetDocument;

/** Składa dokument PDF zestawienia w języku {@link LineSheetDocument#language()}. Adapter: Etap 3. */
public interface LineSheetDocumentRenderer {

    byte[] render(LineSheetDocument document);
}
