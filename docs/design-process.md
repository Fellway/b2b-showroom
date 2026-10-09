# Proces projektowy (wtyczka Impeccable)

UI/UX projektujemy wtyczką **Impeccable** (`/impeccable`). Ten dokument zapisuje, jak powstał obecny system, gdzie
leżą jego artefakty i jak prowadzić kolejne prace projektowe tak, by nie rozjechały się z tym, co już ustalono.

## Źródła prawdy

| Plik | Rola | Kto zmienia |
|---|---|---|
| `PRODUCT.md` | produkt: użytkownicy, sytuacje, pozycjonowanie, zasady, materiały marki, decyzje marki | wywiad `init` / decyzje użytkownika |
| `DESIGN.md` + `.impeccable/design.json` | system wizualny wyprowadzony ze zbudowanego kodu (tokeny, komponenty, zasady) | agent dokumentujący po zakończonej budowie |
| `.impeccable/surfaces/*.md` | brief powierzchni: zakres, kontrakt kierunku (THESIS, OWN-WORLD, STORY, FIRST VIEWPORT, FORM, FINISH), decyzje i otwarte tematy | przed budową nowej powierzchni |
| `frontend/src/app/globals.css` | tokeny w kodzie | razem z `DESIGN.md` |

Kontraktu kierunku nie kopiujemy do kodu ani do treści wysyłanych do przeglądarki.

## Historia

### Etap 4a — katalog publiczny (2026-10-06)
1. **`init`** — wywiad z użytkownikiem → `PRODUCT.md` (marka Fabian's Style, kupcy butików w 4 sytuacjach, wyróżniki, rynki, ton). Strony marki na Facebooku/Instagramie bez logowania nie udostępniają treści — wszystko pochodzi od użytkownika.
2. **Materiały marki** w `brand-assets/` → logo z usuniętym tłem (`frontend/public/brand/`, z zapisanym pochodzeniem w metadanych PNG), opis zdjęć w `PRODUCT.md`.
3. **Runda kierunku** (klucz losowania `20a4c91b`, tryb Persuade): przydzielony „Stół krojczy” (pracownia krojowa), alternatywy i wyzwania z katalogu światów. **Użytkownik wybrał standard kategorii** i wskazał poprzeczkę **COS / Arket**.
4. Budowa code-led (brak generowania obrazów w środowisku → bez makiet graficznych), kontrakt w `.impeccable/surfaces/frontend-src-app-locale-page-tsx.md`.
5. **Przegląd końcowy** (niezależny recenzent): „fix” → 8 poprawek (m.in. bez dekoracyjnego rozmycia, pierwszy rząd cen w ekranie 1440×900, znacznik „w zestawieniu” na kartach, neutralna językowo nazwa opiekuna) → druga runda → domknięcie.
6. **Dokumentacja:** `DESIGN.md` napisany ze zbudowanego kodu.

### Etap 4b — panel administracyjny (2026-10-09)
1. Spłata długu systemu z przeglądu 4a: token `edge` (kontrast krawędzi kontrolek 3,6:1), prymitywy shadcn dopasowane, jeden cień menu.
2. Pytania o zadania (Operate) → jedna osoba, często; tłumaczenia wklejane; desktop teraz, mobile później.
3. **Runda struktury** w ustalonym świecie (klucz `767993ca`, rozdane: pulpit dnia / sekcje + pełny edytor / lista + edytor obok). Strona decyzyjna zamknięta bez odpowiedzi → to samo pytanie narzędziem pytań → **„Sekcje + pełny edytor”**.
4. Kontrakt w `.impeccable/surfaces/frontend-src-app-admin-panel-layout-tsx.md`; budowa w trzech równoległych częściach.
5. **Przegląd:** „fix” → 8 poprawek (usuwanie jako tekst zamiast czerwieni, statystyki jako wiersze danych zamiast kafelków, sygnał braku zdjęć, polska odmiana liczebników) → druga runda; pozostałe drobiazgi domknięte i zweryfikowane zrzutami bez trzeciej rundy recenzenta.
6. `DESIGN.md` rozszerzony o wzorce panelu.

### Baner zgody (2026-10-09)
Rozszerzenie istniejącej powierzchni (bez nowej rundy kierunku). Przegląd: „fix” (m.in. „losowy” zamiast „anonimowy” identyfikator, powrót fokusu, szerokość = kolumna siatki) → **ship**. Dodatkowo naprawione przepełnienie przycisków UK przy 768 px.

## Jak prowadzić kolejne prace projektowe

- **Rozszerzenie istniejącej powierzchni** (nowy komponent, stan, sekcja): dziedziczy świat z `DESIGN.md` — bez nowej rundy kierunku; przed edycją UI przeczytaj `craft-floor` wtyczki.
- **Nowa cała powierzchnia** (np. wersja mobilna panelu, strona polityki prywatności): pytania o zadanie → runda struktury (`concept-seed --scope surface`) → brief z kontraktem → budowa → przegląd końcowy (maks. dwie rundy poprawek) → aktualizacja `DESIGN.md`.
- **Zmiana systemu** (nowy token, nowa zasada): tylko świadomie, z aktualizacją `DESIGN.md` i `.impeccable/design.json`.
- Zasady, których pilnujemy: czerń jako jedyny kolor interfejsu (czerwień tylko dla błędów), zero zaokrągleń (okrąg tylko jako kropka stanu), wagi 400/500, cyfry tabelaryczne w danych, cień tylko na warstwach nałożonych, bez plakietek promocyjnych, bez nagłówków-etykiet nad tytułami (eyebrow), ikony Lucide 1.5.
- Każdy nowy raster w `frontend/public` musi mieć zapisane pochodzenie (`impeccable embed-prompt --scan frontend/public`).

## Znane drobiazgi z przeglądów (do rozważenia przy okazji)

- Paski rankingu w statystykach kończą się przed kolumną wartości (długość liczona względem kolumny nazwy).
- Pasek zapisu w edytorze ma wpisane na sztywno przesunięcie zamiast tokenu.
- Pola dat w statystykach mają 32 px wysokości (reszta pól 40 px) i natywną ikonę kalendarza przeglądarki.
- Pomysł: licznik nowych zestawień od ostatniej wizyty w nawigacji panelu.
