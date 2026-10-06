package pl.wizytowka.linesheet.application.port.in;

import pl.wizytowka.linesheet.domain.LineSheetId;

public interface ResendLineSheetEmailUseCase {

    /** Ponownie wysyła zestawienie na adres klienta (np. po nieudanej próbie). Wysyłka następuje po zatwierdzeniu transakcji. */
    void resend(LineSheetId id);
}
