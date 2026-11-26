package pl.wizytowka.legal.application.port.out;

import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalDocumentVersion;
import pl.wizytowka.legal.domain.LegalDocumentVersionId;

import java.util.List;
import java.util.Optional;

public interface LegalDocumentRepository {

    Optional<LegalDocumentVersion> findById(LegalDocumentVersionId id);

    /** Opublikowane wersje, od najnowszej. */
    List<LegalDocumentVersion> findPublished(LegalDocumentType type);

    Optional<LegalDocumentVersion> findLatestPublished(LegalDocumentType type);

    Optional<LegalDocumentVersion> findDraft(LegalDocumentType type);

    boolean existsAny(LegalDocumentType type);

    /**
     * Zapisuje szkic albo jego publikację. Treść opublikowanej wersji nie jest już nadpisywana.
     * @throws pl.wizytowka.shared.domain.ConcurrentModificationException przy niezgodności wersji
     */
    void save(LegalDocumentVersion version);

    void delete(LegalDocumentVersion version);
}
