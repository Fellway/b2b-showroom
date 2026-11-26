package pl.wizytowka.legal.application.view;

import pl.wizytowka.legal.domain.LegalDocumentType;

import java.util.List;

/**
 * Stan jednego rodzaju dokumentu w panelu.
 *
 * @param current   obowiązująca (najnowsza opublikowana) wersja albo {@code null}
 * @param draft     szkic następnej wersji albo {@code null} (najwyżej jeden na rodzaj)
 * @param published historia opublikowanych wersji, od najnowszej
 */
public record AdminLegalOverview(LegalDocumentType type,
                                 LegalVersionSummary current,
                                 LegalVersionSummary draft,
                                 List<LegalVersionSummary> published) {
}
