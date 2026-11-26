package pl.wizytowka.linesheet.application.port.out;

import pl.wizytowka.linesheet.domain.LineSheet;
import pl.wizytowka.linesheet.domain.LineSheetId;

import java.util.List;
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

    /**
     * Usuwa dane osoby kontaktowej, wiadomość, zgodę i odwołanie do PDF z zestawień kupca oraz odłącza je od konta.
     * @return klucze plików PDF, które należy usunąć z magazynu
     */
    List<String> anonymizeOfBuyer(UUID buyerAccountId);
}
