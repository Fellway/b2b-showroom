package pl.wizytowka.analytics.application.view;

import java.util.UUID;

/**
 * Pozycja rankingu produktów.
 * @param known {@code false}, gdy produkt został usunięty z katalogu — pola opisowe są wtedy {@code null}
 * @param status status produktu w katalogu (DRAFT/PUBLISHED/HIDDEN/ARCHIVED) lub {@code null}
 */
public record ProductRankingEntry(UUID productId, boolean known, String slug, String styleCode, String name,
                                  String status, long count) {
}
