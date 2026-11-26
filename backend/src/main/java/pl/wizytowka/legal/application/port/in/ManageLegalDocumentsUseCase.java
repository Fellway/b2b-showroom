package pl.wizytowka.legal.application.port.in;

import pl.wizytowka.legal.application.view.AdminLegalOverview;
import pl.wizytowka.legal.application.view.AdminLegalVersionView;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalDocumentVersionId;
import pl.wizytowka.legal.domain.LegalText;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dokumenty prawne w panelu: szkic → publikacja (D25). Opublikowanych wersji nie da się zmienić ani usunąć. */
public interface ManageLegalDocumentsUseCase {

    List<AdminLegalOverview> overview();

    AdminLegalVersionView version(LegalDocumentVersionId id);

    /**
     * Tworzy szkic następnej wersji — kopię obowiązującej treści (albo pusty, gdy nic nie opublikowano).
     * @throws pl.wizytowka.shared.domain.ResourceConflictException gdy szkic tego dokumentu już istnieje
     */
    LegalDocumentVersionId createDraft(LegalDocumentType type);

    void updateDraft(LegalDocumentVersionId id, long expectedVersion, Map<CatalogLanguage, LegalText> texts);

    /**
     * @param requireAcceptance czy kupcy muszą zaakceptować nową wersję regulaminu przed dalszym korzystaniem
     *                          z cen i zestawień (ignorowane dla polityki prywatności)
     */
    void publish(LegalDocumentVersionId id, long expectedVersion, boolean requireAcceptance, UUID actor);

    void deleteDraft(LegalDocumentVersionId id);

    /**
     * Pierwsze uruchomienie: publikuje wzór dokumentu jako wersję 1, jeśli dokument nie ma jeszcze żadnej wersji.
     * @return {@code true}, gdy wzór został opublikowany
     */
    boolean publishTemplateIfEmpty(LegalDocumentType type, Map<CatalogLanguage, LegalText> texts);
}
