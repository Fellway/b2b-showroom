package pl.wizytowka.linesheet.application.port.in;

import pl.wizytowka.linesheet.application.view.CreatedLineSheet;

public interface CreateLineSheetUseCase {

    /**
     * Tworzy zestawienie, generuje i zapisuje PDF, a na życzenie klienta wysyła go e-mailem po zatwierdzeniu transakcji.
     * @throws pl.wizytowka.shared.domain.BusinessRuleViolationException gdy wybrany wariant nie jest już dostępny
     */
    CreatedLineSheet create(CreateLineSheetCommand command);
}
