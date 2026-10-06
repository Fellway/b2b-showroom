package pl.wizytowka.linesheet.application.port.out;

import pl.wizytowka.linesheet.domain.LineSheet;
import pl.wizytowka.linesheet.domain.LineSheetId;

import java.util.Optional;
import java.util.UUID;

public interface LineSheetRepository {

    Optional<LineSheet> findById(LineSheetId id);

    Optional<LineSheet> findByPublicId(UUID publicId);

    /**
     * Nowe zestawienie zapisywane jest w całości; dla istniejącego aktualizowany jest wyłącznie stan techniczny
     * (dołączony PDF) — pozycje są niezmienne.
     * @throws pl.wizytowka.shared.domain.ConcurrentModificationException przy niezgodności wersji
     */
    void save(LineSheet lineSheet);
}
