package pl.wizytowka.linesheet.application.port.out;

import pl.wizytowka.linesheet.application.view.LineSheetDocument;

/**
 * Wysyła zestawienie z załączonym PDF na adres {@code document.recipient().email()}. Adapter: Etap 3.
 * Zgłasza wyjątek, gdy wiadomości nie udało się przekazać do serwera pocztowego.
 */
public interface LineSheetMailer {

    void send(LineSheetDocument document, byte[] pdf);
}
