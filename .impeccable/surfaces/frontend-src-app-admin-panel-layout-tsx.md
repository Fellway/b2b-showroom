---
version: 1
slug: "frontend-src-app-admin-panel-layout-tsx"
primary_target: "frontend/src/app/admin/(panel)/layout.tsx"
related_targets: ["frontend/src/app/admin/(panel)/products/page.tsx"]
---

## Scope

Panel administracyjny katalogu Fabian's Style (`/admin`, poza routingiem językowym, po polsku). Tryb: **Operate**.
Dziedziczy świat z `DESIGN.md` (bez zmian systemu poza spłaconym długiem: token `edge`, prymitywy shadcn).

## Audience, task, states

Jedna osoba (właściciel / menedżer marki), kilka razy w tygodniu, desktop. Zadania: nowe modele ze zdjęciami,
ceny i ukrywanie, przegląd zestawień (leady), statystyki; tłumaczenia EN/CS/UK wklejane z zewnątrz — braki widoczne.
Stany: ładowanie (wiersze zastępcze), pusto, błąd z ponowieniem, zapis (blokada przycisku + potwierdzenie „Zapisano”),
konflikt wersji (komunikat + odświeżenie), brak uprawnień (EDITOR bez kont). Przyszłość: wersja mobilna i aplikacja —
logika wyłącznie w REST API, układy z dwóch kolumn składają się w jedną.

## Direction contract

THESIS: Panel to to samo narzędzie co katalog, tylko odwrócone na zaplecze — sekcje jako ciche tabele i model
edytowany na pełnej stronie w krokach. Odrzuca dashboardowy chrom (kafelki metryk, kolorowe statusy, karty z cieniem).

OWN-WORLD: świat `DESIGN.md` bez zmian: biel, czerń jako jedyny kolor interfejsu, `well` dla podkładów i hover,
linie `line` między wierszami, `edge` dla krawędzi kontrolek; nawigacja boczna 232 px z aktywną pozycją w negatywie;
status modelu jako tekst + kropka (pełna = widoczny w katalogu); miniatury zdjęć 2:3.

STORY: administrator wchodzi na listę modeli, widzi status, cenę, braki tłumaczeń i zdjęć; otwiera model, w krokach
uzupełnia dane, wkleja tłumaczenia obok polskiego wzorca, buduje siatkę kolor × rozmiar z SKU/EAN, wgrywa zdjęcia
i publikuje; w zestawieniach widzi leady z kontaktem i PDF.

FIRST VIEWPORT: lewa kolumna nawigacji (logo, Modele / Zestawienia / Statystyki, Słowniki, Ustawienia, użytkownik
i wylogowanie na dole); po prawej przyklejony nagłówek strony 64 px (tytuł, licznik, akcja „Nowy model” w czerni)
i od razu tabela modeli: miniatura 2:3, nazwa + kod, kategoria, status, cena hurtowa, warianty, braki tłumaczeń.

FORM: sekcje + pełnoekranowy edytor (struktura wybrana przez użytkownika z rozdania); seed key 767993ca.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## Unresolved

- Wersja mobilna panelu (później).
