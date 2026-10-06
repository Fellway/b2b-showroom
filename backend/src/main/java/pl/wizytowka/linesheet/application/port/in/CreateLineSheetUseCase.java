package pl.wizytowka.linesheet.application.port.in;

import pl.wizytowka.linesheet.application.view.CreatedLineSheet;

public interface CreateLineSheetUseCase {

    /**
     * Tworzy zestawienie oraz generuje i zapisuje jego PDF (do pobrania przez klienta).
     * @throws pl.wizytowka.shared.domain.BusinessRuleViolationException gdy wybrany wariant nie jest już dostępny
     */
    CreatedLineSheet create(CreateLineSheetCommand command);
}
