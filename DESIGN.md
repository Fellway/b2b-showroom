---
name: Fabian's Style — showroom hurtowy
description: Cichy, prowadzony zdjęciem katalog hurtowy odzieży damskiej; jedynym kolorem jest kolekcja.
colors:
  paper: "#ffffff"
  ink: "#111111"
  well: "#f2f1ef"
  ink-2: "#474644"
  ink-3: "#6b6965"
  edge: "#8a8782"
  line: "#e3e1dd"
  scrollbar: "#c9c6c1"
  danger: "#a1271c"
  swatch-edge: "rgb(0 0 0 / 0.15)"
typography:
  display:
    fontFamily: "Golos Text, ui-sans-serif, system-ui, sans-serif"
    fontSize: "2rem"
    fontWeight: 500
    lineHeight: "2.4rem"
    letterSpacing: "-0.02em"
  title:
    fontFamily: "Golos Text, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.375rem"
    fontWeight: 500
    lineHeight: "1.8rem"
  body:
    fontFamily: "Golos Text, ui-sans-serif, system-ui, sans-serif"
    fontSize: "0.9375rem"
    fontWeight: 400
    lineHeight: "1.5rem"
  small:
    fontFamily: "Golos Text, ui-sans-serif, system-ui, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 400
    lineHeight: "1.25rem"
  meta:
    fontFamily: "Golos Text, ui-sans-serif, system-ui, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 400
    lineHeight: "1.1rem"
  data:
    fontFamily: "Golos Text, ui-sans-serif, system-ui, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 400
    lineHeight: "1.25rem"
    fontFeature: "\"tnum\" 1, \"lnum\" 1"
rounded:
  none: "0px"
  dot: "9999px"
spacing:
  gutter-mobile: "20px"
  gutter-desktop: "32px"
  container: "1600px"
  header: "64px"
  panel-x: "24px"
  control-gap: "6px"
  admin-sidebar: "232px"
  admin-content: "1400px"
  admin-sheet: "520px"
components:
  button-primary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
    typography: "{typography.small}"
    rounded: "{rounded.none}"
    padding: "0 24px"
    height: "48px"
  button-primary-hover:
    backgroundColor: "{colors.ink-2}"
    textColor: "{colors.paper}"
  button-primary-disabled:
    backgroundColor: "{colors.well}"
    textColor: "{colors.ink-3}"
  button-primary-md:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
    typography: "{typography.small}"
    rounded: "{rounded.none}"
    padding: "0 16px"
    height: "40px"
  button-secondary:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    typography: "{typography.small}"
    rounded: "{rounded.none}"
    padding: "0 16px"
    height: "40px"
  button-text:
    textColor: "{colors.ink}"
    typography: "{typography.small}"
    padding: "8px 12px"
  button-danger:
    backgroundColor: "transparent"
    textColor: "{colors.ink-2}"
    typography: "{typography.meta}"
    padding: "0 12px"
    height: "40px"
  button-danger-hover:
    textColor: "{colors.ink}"
  toggle-box:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    typography: "{typography.data}"
    rounded: "{rounded.none}"
    height: "44px"
  toggle-box-active:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
  input-text:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    typography: "{typography.small}"
    rounded: "{rounded.none}"
    padding: "0 12px"
    height: "44px"
  input-admin:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    typography: "{typography.small}"
    rounded: "{rounded.none}"
    padding: "0 12px"
    height: "40px"
  input-admin-disabled:
    backgroundColor: "{colors.well}"
    textColor: "{colors.ink-3}"
  count-badge:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
    typography: "{typography.meta}"
    rounded: "{rounded.none}"
    height: "20px"
  chip-filter:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    typography: "{typography.meta}"
    rounded: "{rounded.none}"
    padding: "0 10px"
    height: "32px"
  stepper:
    backgroundColor: "transparent"
    textColor: "{colors.ink-2}"
    typography: "{typography.data}"
    rounded: "{rounded.none}"
    height: "36px"
  menu-item:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    typography: "{typography.small}"
    padding: "8px 12px"
  menu-item-hover:
    backgroundColor: "{colors.well}"
  note-band:
    backgroundColor: "{colors.well}"
    textColor: "{colors.ink-2}"
    typography: "{typography.meta}"
    padding: "8px 20px"
  admin-nav-item:
    backgroundColor: "transparent"
    textColor: "{colors.ink-2}"
    typography: "{typography.small}"
    rounded: "{rounded.none}"
    padding: "0 8px"
    height: "36px"
  admin-nav-item-hover:
    backgroundColor: "{colors.well}"
    textColor: "{colors.ink}"
  admin-nav-item-active:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
  admin-page-header:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    padding: "12px 32px"
    height: "64px"
  admin-table-head:
    textColor: "{colors.ink-3}"
    typography: "{typography.meta}"
    padding: "4px 12px 8px"
  admin-table-cell:
    textColor: "{colors.ink}"
    typography: "{typography.small}"
    padding: "12px"
  flash:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
    typography: "{typography.small}"
    rounded: "{rounded.none}"
    padding: "12px 16px"
---

# Design System: Fabian's Style — showroom hurtowy

## Overview

**Creative North Star: "Cicha witryna kolekcji"**

Interfejs jest białą ścianą galerii, na której wiszą printy kolekcji. Biel jest tłem, ciepłoszary podkład trzyma miejsce pod zdjęciem, a czerń logo jest jedynym kolorem interfejsu: tekst, stany aktywne i akcja główna. Każdy inny kolor na ekranie należy do produktu (zdjęcia, próbki kolorów). Poprzeczka wykonania to COS / Arket: duże zdjęcia 2:3, dużo powietrza między rzędami, precyzyjna drobna typografia jednej groteski.

Gęstość jest dwubiegunowa i to jest cecha systemu, nie kompromis. Warstwa obrazu (siatka, galeria) oddycha szerokimi odstępami; warstwa danych (ceny, mnożnik, rozmiary, EAN, ilości) jest zwarta, wyrównana cyframi tabelarycznymi i oddzielona włoskowatymi liniami. Ta sama gramatyka danych — etykieta w `ink-3`, wartość w `ink`, linia `line` między wierszami — przenosi się bez zmian do gęstych widoków panelu administracyjnego.

Panel administracyjny to ten sam świat odwrócony na zaplecze, nie drugi system: nawigacja boczna z aktywną pozycją w negatywie, ciche tabele, status jako tekst z kropką, formularze w panelu bocznym i edytor modelu na pełnej stronie. System odrzuca chrom sklepu detalicznego (banery promocji, plakietki „nowość/-30%”, koszyk), wygląd arkusza kalkulacyjnego (siatki z obramowaniem każdej komórki, kolorowe nagłówki tabel) i dashboardowy chrom panelu (kafelki metryk, kolorowe statusy, karty z cieniem). Kontrolki są płaskie, kwadratowe i tekstowe.

**Key Characteristics:**
- Jeden kolor interfejsu (`ink` #111111) na bieli; kolor należy do kolekcji.
- Zero zaokrągleń: zdjęcia, pola, przyciski, panele, menu. Jedyny wyjątek to kropka stanu.
- Jedna groteska (Golos Text, łacina + cyrylica), ładowane i używane wyłącznie wagi 400 i 500, pięć stopni skali od 12 do 32 px.
- Cyfry tabelaryczne we wszystkich danych handlowych.
- Dwie krawędzie: `edge` obrysowuje kontrolki, `line` dzieli treść.
- Głębia przez linię i podkład, nie przez cień; cień tylko na warstwach nakładanych.
- Ikony liniowe Lucide o grubości kreski 1.5, rozmiar 14–16 px; strzałki też są ikonami, nie znakami tekstu.

## Colors

Achromatyczny, lekko ciepły zestaw: biel, pięć stopni ciepłej szarości i czerń; jedyny kolor nasycony to czerwień błędu.

### Primary
- **Czerń logo** (`ink`): cały tekst podstawowy, wypełnienie akcji głównej, stan aktywny rozmiaru, filtra i pozycji nawigacji panelu, licznik w pasku, podkreślenie aktywnej kategorii, kropka „w zestawieniu” i kropka statusu, wypełnienie paska rankingu, tło potwierdzenia „Zapisano”, obrys `:focus-visible` i obrys kontrolki przy hover/fokusie, zaznaczenie tekstu (`::selection` w negatywie).

### Neutral
- **Papier** (`paper`): tło strony, paneli bocznych, nawigacji panelu, menu i pól.
- **Podkład zdjęć** (`well`): tło pod zdjęciem i miniaturą przed załadowaniem, pas noty, blok kontaktu z opiekunem, hover pozycji menu i nawigacji panelu, hover interaktywnego wiersza tabeli (60% krycia), wypełnienie wyłączonej akcji głównej i wyłączonego pola, tło pola ilości w fokusie, bloki zastępcze ładowania.
- **Grafit** (`ink-2`): tekst drugorzędny (lead, opisy, nieaktywna nawigacja, kategoria w tabeli), stan hover akcji głównej, ikony stepperów, akcja nieodwracalna (tekst z podkreśleniem), status szkicu i ukrytego.
- **Szarość etykiet** (`ink-3`): etykiety danych i pól („Hurt”, „MSRP”, nagłówki kolumn), etykiety grup nawigacji, kod modelu, liczniki wyników, meta w nagłówku strony, lista braków, placeholdery, chevrony, status zarchiwizowanego. Kontrast na `paper` ok. 5.5:1, na `well` ok. 4.8:1 — dopuszczalny dla tekstu.
- **Krawędź kontrolki** (`edge`): obrys 1 px każdej kontrolki w spoczynku — pola tekstowe, selecty, pola rozmiaru i przełączniki zakresu, przyciski wyboru koloru, steppery (obrys zewnętrzny), akcja z obrysem, chipy filtrów, strefa upuszczania zdjęć. Kontrast 3.6:1 na bieli spełnia WCAG 1.4.11; kontrolka nie może zniknąć na białym tle.
- **Włoskowata linia** (`line`): każdy podział treści — dół paska nagłówka i nagłówka strony panelu, prawa krawędź nawigacji, obramowanie paska filtrów, separatory wierszy tabel i danych, linia nad grupą pól, wewnętrzne przegrody steppera, obrys menu, dialogu i panelu, tor paska rankingu, obrys wyłączonej kontrolki. Nigdy jako jedyny obrys aktywnej kontrolki.
- **Suwak** (`scrollbar`): kciuk cienkiego paska przewijania (`scrollbar-width: thin`) na przezroczystym torze, globalnie.
- **Krawędź próbki** (`swatch-edge`): 1 px półprzezroczystej czerni wokół każdej próbki koloru produktu, aby jasne kolory nie znikały na bieli.

### Tertiary
- **Czerwień błędu** (`danger`): wyłącznie komunikaty błędu (`role="alert"`) i obrys pola po nieudanej walidacji (`aria-invalid`, `:user-invalid`). Nigdy jako akcent i nigdy na przycisku — także usuwanie nie jest czerwone.

### Named Rules
**The Kolor-należy-do-kolekcji Rule.** Interfejs nie ma koloru akcentu. Wszystko, co nie jest zdjęciem ani próbką produktu, jest w skali `paper` → `ink`; `danger` pojawia się tylko przy błędzie.

**The Negatyw-aktywnego Rule.** Stan wybrany to odwrócenie: tło `ink`, tekst `paper` (pole rozmiaru, licznik filtrów, licznik zestawienia, przełącznik zakresu, aktywna pozycja nawigacji panelu). Nie wprowadzamy drugiego koloru do oznaczania wyboru.

**The Krawędź-czy-linia Rule.** Jeśli element da się kliknąć lub wpisać w niego wartość, jego obrys w spoczynku to `edge`; jeśli tylko dzieli treść, to `line`. Hover i fokus podnoszą obrys kontrolki do `ink`; wyłączenie opuszcza go do `line`.

## Typography

**Display Font:** Golos Text (z ui-sans-serif, system-ui)
**Body Font:** Golos Text (z ui-sans-serif, system-ui)
**Label/Mono Font:** Golos Text z `font-variant-numeric: tabular-nums lining-nums` (klasa `.tabular`)

**Character:** Jedna neutralna, nowoczesna groteska z pełną cyrylicą (ukraiński jest językiem pierwszorzędnym). Hierarchię niesie rozmiar i waga 500, nie zmiana kroju. Kaligraficzne logo to wyłącznie raster marki, nigdy krój.

### Hierarchy
- **Display** (500, 2rem / 2.4rem, tracking -0.02em): tytuł strony katalogu (nazwa kolekcji lub kategorii). Jeden na stronę. Panel nie używa Display.
- **Title** (500, 1.375rem / 1.8rem): nazwa produktu na mobile, tytuł kroku „gotowe” w panelu zestawienia, nagłówek sekcji kontaktu w stopce. Na karcie produktu od `md` nazwa rośnie jednorazowo do 1.75rem / 2.25rem.
- **Body** (400, 0.9375rem / 1.5rem): tekst bazowy dokumentu, lead pod tytułem, cena hurtowa na karcie produktu (500), tytuł panelu bocznego, nagłówka strony panelu, sekcji edytora i dialogu (500). Teksty ciągłe do 62ch.
- **Small** (400/500, 0.8125rem / 1.25rem): domyślny rozmiar interfejsu — nawigacja, filtry, etykiety przycisków, nazwy produktów w siatce i w tabeli (500), wiersze danych, pola formularzy, tytuł grupy pól (500).
- **Meta** (400, 0.75rem / 1.1rem): etykiety pól i podpowiedzi, komunikat błędu pola, nagłówki kolumn, etykiety grup nawigacji, meta w nagłówku strony, kod modelu, liczba kolorów, chipy filtrów, liczniki, nota, akcja nieodwracalna, linki pomocnicze („Wszystkie rozmiary”, „Usuń model”).
- **Data** (jak Small + cyfry tabelaryczne): kwoty, mnożnik (×2,38), rozmiary, EAN, SKU, ilości, numer zestawienia, daty i liczniki w panelu.

### Named Rules
**The Dwie-wagi Rule.** Ładujemy i używamy wyłącznie 400 i 500. Pogrubienie nie jest narzędziem hierarchii.

**The Cyfry-w-kolumnie Rule.** Każda liczba handlowa (cena, mnożnik, rozmiar, EAN, SKU, ilość, licznik) dostaje `.tabular`. Kwoty w kolumnach wyrównujemy do prawej.

## Layout

Kontener `max-width: 1600px`, marginesy boczne 20 px na mobile i 32 px od `md` (768 px). Pasek nagłówka ma stałą wysokość 64 px (`--header-height`), jest przyklejony (`sticky`), biały, z dolną linią; poniżej `lg` (1024 px) kategorie schodzą do drugiego, przewijanego poziomo rzędu z wygaszeniem prawej krawędzi.

Siatka katalogu: 2 kolumny (odstęp 12 px) → 3 kolumny od `md` (16 px) → 4 kolumny od `lg`; odstęp pionowy 40 px, od `lg` 56 px. Karta produktu: od `lg` siatka 12-kolumnowa z odstępem 48 px — galeria 7 kolumn, panel informacji 5 kolumn przyklejony pod nagłówkiem (`top: header + 2rem`); na mobile galeria to poziomy karuzel ze snapem (86vw na zdjęcie).

Rytm pionowy w panelach danych: 24 px między grupami (cena, kolor, rozmiary, akcja), 12 px między etykietą grupy a kontrolkami, 6 px między kontrolkami w siatce rozmiarów. Panele boczne mają poziomy padding 24 px, nagłówek o wysokości paska strony i stopkę akcji odciętą linią.

Panel administracyjny: od `lg` dwie kolumny — nawigacja 232 px przyklejona na pełną wysokość okna, odcięta linią z prawej, i treść. Poniżej `lg` nawigacja staje się górnym paskiem przewijanym poziomo (bez etykiet grup), układ nie rozsypuje się na tablecie. Treść strony: margines 32 px, padding pionowy 32 px, szerokość do 1400 px (formularze wąskie do 48rem). Edytor modelu: kolumna kroków 180 px + odstęp 48 px + treść; sekcje edytora rozdzielone linią i paddingiem 40 px. Rankingi statystyk: trzy kolumny od `xl` z odstępem 48 px.

### Named Rules
**The Dwie-gęstości Rule.** Obraz oddycha (40–56 px między rzędami, 64 px przed stopką strony), dane są zwarte (wiersze 6 px, odstęp w siatce kontrolek 6 px). Nie rozrzedzamy danych dla „powietrza” ani nie zagęszczamy siatki zdjęć dla „wydajności”.

## Elevation & Depth

System jest płaski. Głębię w spoczynku niesie wyłącznie linia `line` i podkład `well`. Cień istnieje tylko na warstwach, które nakładają się na treść (menu rozwijane, dialog, potwierdzenie „Zapisano”, panel boczny), i jest długi, miękki, z ujemnym rozproszeniem — raczej sugestia uniesienia niż obrys. Zdjęcia, karty, pola, przyciski, tabele oraz przyklejone paski (nagłówek, nagłówek strony panelu, pasek zapisu) nigdy nie mają cienia — od treści oddziela je linia. Pod panelem bocznym i dialogiem leży zaciemnienie `rgb(0 0 0 / 0.25)`.

### Shadow Vocabulary
- **Menu** (`box-shadow: 0 12px 32px -16px rgb(17 17 17 / 0.3)`, token `--shadow-menu`): jedyny cień warstw małych — popover filtrów i sortowania, przełącznik języka, dialog potwierdzenia, potwierdzenie „Zapisano”, panel zgody na analitykę.
- **Panel boczny** (`box-shadow: -24px 0 48px -32px rgb(17 17 17 / 0.35)`, token `--shadow-panel`): szuflada zestawienia i panel formularza w panelu administracyjnym, cień rzucany w lewo.

### Named Rules
**The Płasko-w-spoczynku Rule.** Element osadzony w stronie nie ma cienia. Cień dostaje tylko warstwa nałożona na stronę, i tylko jeden z dwóch tokenów.

## Shapes

Kwadratowy świat: wszystkie tokeny promienia (`--radius-sm` … `--radius-xl`, `--radius`) wynoszą 0 px, więc nawet komponenty shadcn/ui renderują się z ostrymi narożnikami. Obrys to 1 px (`edge` dla kontrolki w spoczynku, `line` dla podziału, `ink` aktywnie); grubszego obrysu nie ma poza 2 px pierścieniem fokusu z odstępem 2 px i 2 px paskiem rankingu. Zdjęcia zawsze w proporcji 2:3, przycięte `object-cover`, bez ramek — także miniatury w tabelach (32 × 48 px). Jedyny kształt okrągły to kropka stanu: „w zestawieniu” (6–10 px, pełne koło `ink`, przy próbce z 2 px obwódką `paper`), status w panelu (8 px, pełna lub pusta) i znacznik kroku edytora wymagającego uwagi (6 px, pusta).

### Named Rules
**The Kwadrat-i-kropka Rule.** Narożniki są ostre bez wyjątku; okrąg zarezerwowany jest wyłącznie dla kropki stanu.

## Components

### Buttons
Płaskie i tekstowe; czerń dla jednej akcji, obrys dla akcji obok niej, tekst dla reszty.
- **Shape:** ostre narożniki (0 px), bez cieni i przesunięć.
- **Primary:** wypełnienie `ink`, tekst `paper`, Small 500. W katalogu i stopce panelu bocznego wysokość 48 px, padding 24 px, zwykle na pełną szerokość kolumny („Dodaj do zestawienia”, „Dalej: utwórz PDF”, „Pobierz PDF”, zapis formularza w panelu). W nagłówku strony panelu, pasku zapisu i dialogu wysokość 40 px, padding 16 px („Nowy model”, „Zapisz zmiany”). Jedna na widok.
- **Hover / Focus:** tło przechodzi do `ink-2` (transition-colors); fokus to globalny obrys 2 px `ink` z odstępem 2 px. Wyłączona bez zmian w danych: `well` + `ink-3`. Potwierdzenie po akcji zostaje czarne z ikoną check (zoom-in 300 ms). Wysyłanie: etykieta w trybie ciągłym („Zapisywanie…”, „Usuwanie…”) i blokada.
- **Z obrysem (secondary):** tło `paper`, obrys `edge`, tekst `ink`, Small, 40 px, padding 16 px; hover obrysu do `ink`; wyłączona: obrys `line`, tekst `ink-3`. Akcja równorzędna obok głównej („Pokaż” przy zakresie dat).
- **Tekstowy:** Small w `ink-2` lub `ink`, wysokość 40 px, padding 12 px; hover do `ink` albo pojawienie się podkreślenia (offset 4 px, grubość 1 px). „Anuluj” i „Odrzuć” stoją zawsze tekstem obok akcji głównej. Linki pomocnicze w Meta są podkreślone w spoczynku i tracą podkreślenie przy hover.
- **Nieodwracalny (danger):** Meta `ink-2`, podkreślony (offset 4 px), padding 12 px; hover do `ink`, wyłączony traci podkreślenie. Nie jest czerwony. Otwiera dialog potwierdzenia, nigdy nie wykonuje akcji od razu.
- **Ikona:** kwadrat 32–36 px, ikona Lucide 14–16 px w `ink-3`/`ink-2`, hover do `ink`. Strzałki (powrót, paginacja) to ikony Lucide `ArrowLeft`/`ArrowRight` 14 px obok tekstu.

### Chips
- **Style:** filtr aktywny jako prostokąt 32 px, obrys `edge`, Meta, ikona X 14 px; hover obrysu do `ink`.
- **State:** usuwa filtr kliknięciem. Zapytanie wyszukiwania wyświetla się jako ten sam prostokąt bez X, z obrysem `line`, bo nie jest kontrolką.

### Cards / Containers
- **Corner Style:** 0 px.
- **Background:** karta produktu nie ma tła ani ramki — to zdjęcie 2:3 na `well` i blok tekstu z odstępem 12 px pod nim. Bloki pomocnicze (kontakt z opiekunem) to płaszczyzna `well` z paddingiem 20 px.
- **Shadow Strategy:** brak (patrz Elevation & Depth).
- **Border:** brak na karcie; sekcje danych odcinane liniami góra/dół.
- **Internal Padding:** 20 px (blok `well`), 24 px (panele).
- **Hover karty:** zdjęcie powiększa się do 1.025 przez 700 ms `ease-out-expo`, nazwa dostaje podkreślenie.

### Inputs / Fields
- **Style:** obrys 1 px `edge`, 0 px, padding 12 px, Small; etykieta nad polem w Meta `ink-3` z odstępem 6 px. W katalogu tło przezroczyste, wysokość 44 px; w panelu tło `paper`, wysokość 40 px (pola liczbowe `.tabular` wyrównane do prawej, select z chevronem 16 px `ink-3` po prawej, pole wielowierszowe z regulacją wysokości). Pole wyszukiwania w pasku filtrów katalogu jest bez obrysu — ikona lupy 16 px i tekst na linii paska; w panelu wyszukiwarka to zwykłe pole 40 px z lupą wewnątrz.
- **Podpowiedź:** Meta `ink-3` pod polem, połączona przez `aria-describedby`; znika, gdy pojawia się błąd.
- **Focus:** obrys przechodzi do `ink` (w panelu także przy hover); pole ilości dostaje tło `well`.
- **Error / Disabled:** obrys `danger` po nieudanej walidacji (`aria-invalid`, `:user-invalid`), komunikat w Meta `danger` pod polem z `role="alert"`. Wyłączone: obrys `line`, tło `well`, tekst `ink-3`. Zgoda i przełączniki to natywny checkbox 16 px z `accent-color: ink`.
- **Grupa pól:** linia `line` nad grupą, tytuł Small 500, odstęp 16 px, bez tła (np. jeden język tłumaczenia).

### Toggle box (rozmiar)
Kwadratowe pole wyboru z kodem rozmiaru: obrys `edge`, wysokość 44 px (karta produktu) lub 40 px (menu filtrów), Data, siatka 6 kolumn (4 w menu) z odstępem 6 px. Hover: obrys `ink`. Wybrany: negatyw (`ink` tło, `paper` tekst), `aria-pressed`. Wzorzec obowiązuje dla każdego wyboru wielu krótkich wartości; jako wybór jednej z kilku wartości (zakres 7 / 30 / 90 dni w statystykach) pola stykają się obrysami w jeden rząd 32 px w Meta.

### Próbka koloru
W wierszu danych: kwadrat 10 px z krawędzią `swatch-edge`. Jako przycisk wyboru: 40 px, obrys 1 px `edge` (aktywny `ink`), wewnętrzny odstęp 4 px, próbka z krawędzią 10%. Kropka stanu w prawym górnym rogu oznacza kolor obecny w zestawieniu.

### Stepper ilości
Obrys `edge` wokół całości, przyciski −/+ 32–36 px z ikonami 14 px w `ink-2`, pole liczby 44–48 px szerokości oddzielone wewnętrznymi liniami `line`, Data wyśrodkowana. Minus wyłączony przy zerze (`line`). Puste pole oznacza „bez ilości”, nie zero.

### Wiersz danych (lista definicji / tabela)
Etykieta w `ink-3`, wartość w `ink` wyrównana do prawej, Data, odstęp wierszy 4–6 px; grupa odcięta liniami góra/dół z paddingiem 16 px. Tabela: nagłówki kolumn w Meta `ink-3` waga 400, wiersze rozdzielone linią górną `line`, komórki 6 px pionowo, bez pionowych linii i bez tła nagłówka. To podstawowy wzorzec dla gęstych widoków panelu. W statystykach wiersze metryk mają 10 px pionowo, wartość Data 500, i układają się w dwie kolumny z odstępem 48 px — zamiast kafelków metryk.

### Licznik
Prostokąt min. 20 × 20 px, Meta, Data. Z wartością: negatyw `ink`/`paper` (pojawia się z zoom-in 300 ms). Zero: obrys `line`, tekst dziedziczony (`ink-2` w pasku).

### Menu rozwijane
Panel 0 px, obrys `line`, cień „Menu”, szerokość 288 px (przełącznik języka 192 px), odstęp 6–8 px od wyzwalacza. Pozycje: Small, padding 8 × 12 px, hover `well`, zaznaczenie ikoną check 16 px po prawej (niewidoczna, ale rezerwująca miejsce, gdy niewybrana). Wyzwalacz: tekst + chevron 14 px w `ink-3`; otwarty dostaje podkreślenie.

### Navigation
Linki kategorii w Small `ink-2`, odstęp 24 px, pełna wysokość paska. Hover: `ink`. Aktywny: `ink` + linia 1 px `ink` u dołu, rozwijana od lewej (`scale-x`, 300 ms `ease-out-expo`). Paginacja: tekst „poprzednia / strona n z m / następna” ze strzałkami Lucide nad linią górną, bez numerów stron w kafelkach.

### Nawigacja panelu
Kolumna 232 px na `paper`, odcięta linią z prawej. U góry pas 64 px z logo (raster, 24 px wysokości) i słowem „Panel” w Meta `ink-3`, odcięty linią. Pozycje: Small `ink-2`, wysokość 36 px, padding 8 px, ikona Lucide 16 px z odstępem 10 px; hover tło `well` i tekst `ink`; aktywna w negatywie (`ink` tło, `paper` tekst) z `aria-current="page"`. Grupy (Słowniki, Ustawienia) poprzedza etykieta Meta `ink-3`, odstęp między grupami 24 px. Na dole, odcięty linią: nazwa użytkownika (500), rola i e-mail w Meta `ink-3`, linki „Katalog publiczny” i „Wyloguj” w `ink-2` z ikonami.

### Nagłówek strony panelu
Przyklejony u góry treści, `paper`, linia dolna, min. wysokość 64 px, padding 12 × 32 px. Po lewej tytuł Body 500 i obok, na linii bazowej, meta w Meta `ink-3` (licznik, zakres dat); nad tytułem opcjonalny powrót: Meta `ink-3` ze strzałką Lucide 14 px. Po prawej akcje z odstępem 8 px — co najwyżej jedna czarna.

### Tabela panelu
Gramatyka wiersza danych w pełnej tabeli: Small, nagłówki Meta `ink-3` 400 bez tła, wiersze z linią górną, komórki 12 px pionowo i 12 px poziomo (pierwsza i ostatnia bez zewnętrznego paddingu). Liczby `.tabular` wyrównane do prawej. Miniatura modelu 32 × 48 px (2:3) na `well`, przy braku zdjęcia ikona 14 px `ink-3`. Nazwa Small 500, pod nią kod w Meta `ink-3`; nazwa jest linkiem z podkreśleniem przy hover. Wiersz interaktywny (cały otwiera rekord): hover i fokus wewnątrz tła `well` w 60%. Akcje wiersza tekstem z ikoną po prawej.

### Status (tekst + kropka)
Status zawsze jako słowo z kropką 8 px przed nim, odstęp 8 px. Pełna kropka `ink` i tekst `ink` = widoczny w katalogu (Opublikowany) lub aktywny; pusta kropka z obrysem `ink` i tekst `ink-2` = niewidoczny (Szkic, Ukryty); pusta kropka i tekst `ink-3` = zarchiwizowany lub nieaktywny. Tekst niesie znaczenie, kropka tylko je wzmacnia.

### Lista braków
Kolumna „Braki” w tabeli modeli: Meta, każdy brak w osobnej linii „brak: tłumaczenia: EN, CS, UK” / „brak: zdjęcia” / „brak: warianty” w `ink-3`; komplet jako jedno słowo „komplet” w `ink-2`. Bez ikon ostrzegawczych i bez koloru.

### Panel boczny (szuflada)
Wysuwany z prawej, pełna szerokość na mobile, maks. 480 px od `sm` (zestawienie w katalogu) lub 520 px (formularz w panelu administracyjnym). Nagłówek 64 px z linią dolną, tytuł Body 500 (z licznikiem `ink-3` w zestawieniu); opcjonalny opis Small `ink-2`; treść przewijana z paddingiem 24 × 20 px i odstępem 20 px między polami; stopka odcięta linią: błąd zapisu nad akcjami, akcja nieodwracalna po lewej, „Anuluj” tekstem i akcja główna 48 px po prawej. Kroki (lista → formularz → gotowe) zmieniają treść panelu zamiast otwierać kolejne okna; lista pod panelem zostaje widoczna pod zaciemnieniem.

### Edytor modelu
Pełna strona: po lewej kolumna kroków 180 px przyklejona pod nagłówkiem — lista z linią `line` z lewej, aktywny krok z linią `ink` i tekstem `ink`, pozostałe `ink-2`, pod nazwą notatka w Meta `ink-3`, krok wymagający uwagi z pustą kropką 6 px. Sekcje: tytuł Body 500, opis `ink-3` do 70ch, akcje sekcji po prawej. Pasek zapisu: przyklejony do dołu, `paper`, linia górna, bez cienia, wjeżdża z dołu (300 ms) po pierwszej zmianie; komunikat stanu Small `ink-2` (błąd w `danger`, `role="alert"`), „Odrzuć” tekstem i akcja główna 40 px.

### Dialog potwierdzenia
Tylko przed operacją nieodwracalną. Panel 0 px, maks. 448 px, obrys `line`, cień „Menu”, padding 24 px, na zaciemnieniu. Tytuł Body 500, treść Small `ink-2`, akcje po prawej: „Anuluj” tekstem i akcja główna w czerni z jawnym czasownikiem i obiektem („Usuń model”, „Usuń kolor”) — nigdy „OK” ani „Tak”, nigdy czerwona.

### Potwierdzenie „Zapisano”
Prostokąt `ink` z tekstem Small `paper`, padding 12 × 16 px, ikona check 16 px, w prawym dolnym rogu (24 px od krawędzi), cień „Menu”, wjeżdża z dołu (300 ms), znika po ok. 2.6 s, ogłaszany przez `aria-live="polite"`.

### Stany pusty / błąd / ładowanie
- **Pusty:** linia górna, padding 64 px pionowo, tytuł Body 500, opis `ink-2` do 56ch, opcjonalna akcja; wyrównany do lewej.
- **Błąd:** ten sam układ, komunikat w `danger` (`role="alert"`), „Spróbuj ponownie” jako podkreślony tekst.
- **Ładowanie:** wiersze zastępcze w rytmie tabeli — linia górna, padding 12 px, bloki `well` (miniatura 32 × 48, dwa paski tekstu); bez spinnerów i bez pulsowania.

### Panel zgody (prywatność)
Nieblokujące pytanie o zgodę na analitykę. Desktop: pływająca karta w lewym dolnym rogu (24/32 px od krawędzi), szerokość jednej kolumny siatki katalogu od `lg`, dwóch kolumn na tablecie (`md`) — żaden tytuł produktu nie jest przecinany, a etykiety przycisków mieszczą się we wszystkich językach, `paper`, obrys `line`, cień `--shadow-menu`, padding 20 px; mobile: pełnoszeroki pas u dołu z linią górną. Tytuł Small 500, treść Small `ink-2`, informacja o tym, co zapamiętujemy zawsze, w Meta `ink-3`. Dwie akcje **równej wagi** (`secondary`, siatka 2 kolumny) — nigdy wypełnione „Akceptuję”. Zamknięcie (X) tylko w trybie ustawień, gdy decyzja już istnieje; Escape zamyka, fokus wraca do linku „Ustawienia prywatności” w stopce. Link do polityki prywatności tylko, gdy marka ją dostarczy.

### Pas noty
Pas `well` z tekstem Meta `ink-2`, padding 8 × 20 px. W katalogu pełnoszeroki i wyśrodkowany, dla komunikatów o całej stronie (dane przykładowe); w panelu wyrównany do lewej pod danymi, które objaśnia (np. jak liczone są odsłony). Nie dla promocji.

### Pasek rankingu
Pod nazwą modelu w rankingu: tor 2 px w `line`, wypełnienie `ink` proporcjonalne do maksimum listy, ostre końce. Numer pozycji w Data `ink-3`, wartość Data 500 wyrównana do prawej. Jedyna wizualizacja danych w panelu — bez wykresów kolorowych.

## Do's and Don'ts

### Do:
- **Do** trzymaj każdy element interfejsu w skali `paper` / `well` / `line` / `edge` / `ink-3` / `ink-2` / `ink`; kolor wnoszą tylko zdjęcia i próbki produktu.
- **Do** obrysowuj każdą kontrolkę w spoczynku kolorem `edge` (3.6:1), a `line` zostaw dla separatorów treści.
- **Do** oznaczaj wybór negatywem (`ink` tło, `paper` tekst) i obrysem `ink`, a stan „już dodane” lub „widoczny” kropką `ink`.
- **Do** pokazuj status jako tekst z kropką (pełna = widoczny, pusta = niewidoczny).
- **Do** dawaj `.tabular` każdej liczbie handlowej i wyrównuj kwoty w kolumnach do prawej.
- **Do** oddzielaj grupy danych linią 1 px `line`, nie ramką ani tłem.
- **Do** używaj jednej czarnej akcji głównej na widok (48 px w katalogu i stopce panelu, 40 px w nagłówku strony, pasku zapisu i dialogu); pozostałe akcje jako obrys `edge`, tekst lub tekst z podkreśleniem.
- **Do** zapisuj akcję nieodwracalną podkreślonym tekstem Meta `ink-2` i potwierdzaj ją dialogiem z czarną akcją nazwaną czasownikiem i obiektem.
- **Do** pokazuj zdjęcia w 2:3 na podkładzie `well`, bez zaokrągleń, ramek i cieni — także miniatury 32 × 48 px.
- **Do** używaj ikon liniowych Lucide z `strokeWidth` 1.5, 14–16 px, zawsze obok tekstu lub z `aria-label`; strzałki również jako ikony Lucide.
- **Do** animuj krótko i z `ease-out-expo` (`cubic-bezier(0.16, 1, 0.3, 1)`), 300 ms dla stanów, 700 ms dla zdjęć; honoruj `prefers-reduced-motion`.

### Don't:
- **Don't** dodawaj koloru akcentu do interfejsu ani nie koloruj stanów (sukces, nowość, promocja, status modelu) — wyjątek to `danger` przy błędzie.
- **Don't** barw akcji usuwania na czerwono; `danger` należy do komunikatów błędu i błędnych pól, nie do przycisków.
- **Don't** obrysowuj kontrolki samą linią `line` — znika na bieli (poniżej 3:1).
- **Don't** zaokrąglaj narożników; okrąg jest tylko kropką stanu.
- **Don't** dawaj cienia elementom osadzonym w stronie (karty, pola, przyciski, zdjęcia, tabele, przyklejone paski) ani nie wprowadzaj trzeciego cienia poza `--shadow-menu` i `--shadow-panel`.
- **Don't** ładuj ani nie używaj wag powyżej 500 ani drugiego kroju; logo nie jest krojem i nie odtwarzamy go fontem.
- **Don't** używaj znaków tekstowych jako strzałek ani ikon (→, ←, ✓) w interfejsie.
- **Don't** obramowuj każdej komórki tabeli ani nie dawaj nagłówkom tła — to wygląd arkusza, który system odrzuca.
- **Don't** buduj panelu z kafelków metryk, kart z cieniem ani kolorowych wykresów; metryki to wiersze danych, ranking to pasek `ink` na torze `line`.
- **Don't** wprowadzaj plakietek „nowość/-30%”, banerów promocji ani metafory koszyka.
