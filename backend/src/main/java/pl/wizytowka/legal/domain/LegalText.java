package pl.wizytowka.legal.domain;

import pl.wizytowka.shared.domain.Guard;

/**
 * Treść dokumentu w jednym języku. Treść to Markdown w ograniczonym zakresie (nagłówki, akapity, listy, pogrubienie,
 * linki) — frontend renderuje go bez HTML-a, więc treść z panelu nie może wstrzyknąć skryptu.
 *
 * @param changeSummary krótkie „co się zmieniło” pokazywane kupcom przy nowej wersji (opcjonalne)
 */
public record LegalText(String title, String body, String changeSummary) {

    public static final int MAX_TITLE = 200;
    public static final int MAX_BODY = 100_000;
    public static final int MAX_SUMMARY = 1_000;

    public LegalText {
        title = Guard.requiredText(title, MAX_TITLE, "legal.text.title");
        body = Guard.requiredText(body, MAX_BODY, "legal.text.body");
        changeSummary = Guard.optionalText(changeSummary, MAX_SUMMARY, "legal.text.change-summary");
    }
}
