---
name: Fabian's Style — showroom hurtowy
description: Cichy, prowadzony zdjęciem katalog hurtowy odzieży damskiej; jedynym kolorem jest kolekcja.
colors:
  paper: "#ffffff"
  ink: "#111111"
  well: "#f2f1ef"
  ink-2: "#474644"
  ink-3: "#6b6965"
  line: "#e3e1dd"
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
  button-text:
    textColor: "{colors.ink}"
    typography: "{typography.small}"
    padding: "8px 12px"
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
---

# Design System: Fabian's Style — showroom hurtowy

## Overview

**Creative North Star: "Cicha witryna kolekcji"**

Interfejs jest białą ścianą galerii, na której wiszą printy kolekcji. Biel jest tłem, ciepłoszary podkład trzyma miejsce pod zdjęciem, a czerń logo jest jedynym kolorem interfejsu: tekst, stany aktywne i akcja główna. Każdy inny kolor na ekranie należy do produktu (zdjęcia, próbki kolorów). Poprzeczka wykonania to COS / Arket: duże zdjęcia 2:3, dużo powietrza między rzędami, precyzyjna drobna typografia jednej groteski.

Gęstość jest dwubiegunowa i to jest cecha systemu, nie kompromis. Warstwa obrazu (siatka, galeria) oddycha szerokimi odstępami; warstwa danych (ceny, mnożnik, rozmiary, EAN, ilości) jest zwarta, wyrównana cyframi tabelarycznymi i oddzielona włoskowatymi liniami. Ta sama gramatyka danych — etykieta w `ink-3`, wartość w `ink`, linia `line` między wierszami — przenosi się bez zmian do gęstych widoków panelu administracyjnego.

System odrzuca chrom sklepu detalicznego (banery promocji, plakietki „nowość/-30%”, koszyk) i wygląd arkusza kalkulacyjnego (siatki z obramowaniem każdej komórki, kolorowe nagłówki tabel). Kontrolki są płaskie, kwadratowe i tekstowe.

**Key Characteristics:**
- Jeden kolor interfejsu (`ink` #111111) na bieli; kolor należy do kolekcji.
- Zero zaokrągleń: zdjęcia, pola, przyciski, panele, menu. Jedyny wyjątek to kropka stanu.
- Jedna groteska (Golos Text, łacina + cyrylica), wagi 400 i 500, pięć stopni skali od 12 do 32 px.
- Cyfry tabelaryczne we wszystkich danych handlowych.
- Głębia przez linię i podkład, nie przez cień; cień tylko na warstwach nakładanych.
- Ikony liniowe Lucide o grubości kreski 1.5, rozmiar 14–16 px.

## Colors

Achromatyczny, lekko ciepły zestaw: biel, cztery stopnie ciepłej szarości i czerń; jedyny kolor nasycony to czerwień błędu.

### Primary
- **Czerń logo** (`ink`): cały tekst podstawowy, wypełnienie akcji głównej, stan aktywny rozmiaru i filtra, licznik w pasku, podkreślenie aktywnej kategorii, kropka „w zestawieniu”, obrys `:focus-visible`, zaznaczenie tekstu (`::selection` w negatywie).

### Neutral
- **Papier** (`paper`): tło strony, paneli bocznych, menu i pól.
- **Podkład zdjęć** (`well`): tło pod zdjęciem przed załadowaniem, pasek noty demonstracyjnej, blok kontaktu z opiekunem, podświetlenie pozycji menu przy najechaniu, wypełnienie wyłączonej akcji głównej, tło pola ilości w fokusie.
- **Grafit** (`ink-2`): tekst drugorzędny (lead, opisy, linki nieaktywnej nawigacji), stan hover akcji głównej, ikony stepperów.
- **Szarość etykiet** (`ink-3`): etykiety danych („Hurt”, „MSRP”, nagłówki kolumn), kod modelu, liczniki wyników, placeholdery, chevrony. Kontrast na `paper` ok. 5.5:1, na `well` ok. 4.8:1 — dopuszczalny dla tekstu.
- **Włoskowata linia** (`line`): każdy podział — dół paska nagłówka, obramowanie paska filtrów, separatory wierszy danych, obrys pól i pól rozmiarów w spoczynku, obrys menu i panelu.
- **Krawędź próbki** (`swatch-edge`): 1 px półprzezroczystej czerni wokół każdej próbki koloru produktu, aby jasne kolory nie znikały na bieli.

### Tertiary
- **Czerwień błędu** (`danger`): wyłącznie komunikaty błędu (`role="alert"`) i obrys pola po nieudanej walidacji. Nigdy jako akcent.

### Named Rules
**The Kolor-należy-do-kolekcji Rule.** Interfejs nie ma koloru akcentu. Wszystko, co nie jest zdjęciem ani próbką produktu, jest w skali `paper` → `ink`; `danger` pojawia się tylko przy błędzie.

**The Negatyw-aktywnego Rule.** Stan wybrany to odwrócenie: tło `ink`, tekst `paper` (pole rozmiaru, licznik filtrów, licznik zestawienia). Nie wprowadzamy drugiego koloru do oznaczania wyboru.

## Typography

**Display Font:** Golos Text (z ui-sans-serif, system-ui)
**Body Font:** Golos Text (z ui-sans-serif, system-ui)
**Label/Mono Font:** Golos Text z `font-variant-numeric: tabular-nums lining-nums` (klasa `.tabular`)

**Character:** Jedna neutralna, nowoczesna groteska z pełną cyrylicą (ukraiński jest językiem pierwszorzędnym). Hierarchię niesie rozmiar i waga 500, nie zmiana kroju. Kaligraficzne logo to wyłącznie raster marki, nigdy krój.

### Hierarchy
- **Display** (500, 2rem / 2.4rem, tracking -0.02em): tytuł strony katalogu (nazwa kolekcji lub kategorii). Jeden na stronę.
- **Title** (500, 1.375rem / 1.8rem): nazwa produktu na mobile, tytuł kroku „gotowe” w panelu, nagłówek sekcji kontaktu w stopce. Na karcie produktu od `md` nazwa rośnie jednorazowo do 1.75rem / 2.25rem.
- **Body** (400, 0.9375rem / 1.5rem): tekst bazowy dokumentu, lead pod tytułem, cena hurtowa na karcie produktu (500), tytuł panelu (500). Teksty ciągłe do 62ch.
- **Small** (400/500, 0.8125rem / 1.25rem): domyślny rozmiar interfejsu — nawigacja, filtry, etykiety przycisków, nazwy produktów w siatce (500), wiersze danych, pola formularzy.
- **Meta** (400, 0.75rem / 1.1rem): etykiety pól, kod modelu, liczba kolorów, chipy filtrów, liczniki, nota demonstracyjna, linki pomocnicze („Wszystkie rozmiary”, „Usuń model”).
- **Data** (jak Small + cyfry tabelaryczne): kwoty, mnożnik (×2,38), rozmiary, EAN, SKU, ilości, numer zestawienia.

### Named Rules
**The Dwie-wagi Rule.** Używamy wyłącznie 400 i 500. Waga 600 jest ładowana, ale nieużywana; pogrubienie nie jest narzędziem hierarchii.

**The Cyfry-w-kolumnie Rule.** Każda liczba handlowa (cena, mnożnik, rozmiar, EAN, SKU, ilość, licznik) dostaje `.tabular`. Kwoty w kolumnach wyrównujemy do prawej.

## Layout

Kontener `max-width: 1600px`, marginesy boczne 20 px na mobile i 32 px od `md` (768 px). Pasek nagłówka ma stałą wysokość 64 px (`--header-height`), jest przyklejony (`sticky`), biały, z dolną linią; poniżej `lg` (1024 px) kategorie schodzą do drugiego, przewijanego poziomo rzędu z wygaszeniem prawej krawędzi.

Siatka katalogu: 2 kolumny (odstęp 12 px) → 3 kolumny od `md` (16 px) → 4 kolumny od `lg`; odstęp pionowy 40 px, od `lg` 56 px. Karta produktu: od `lg` siatka 12-kolumnowa z odstępem 48 px — galeria 7 kolumn, panel informacji 5 kolumn przyklejony pod nagłówkiem (`top: header + 2rem`); na mobile galeria to poziomy karuzel ze snapem (86vw na zdjęcie).

Rytm pionowy w panelach danych: 24 px między grupami (cena, kolor, rozmiary, akcja), 12 px między etykietą grupy a kontrolkami, 6 px między kontrolkami w siatce rozmiarów. Panele boczne mają poziomy padding 24 px, nagłówek o wysokości paska strony i stopkę akcji odciętą linią.

### Named Rules
**The Dwie-gęstości Rule.** Obraz oddycha (40–56 px między rzędami, 64 px przed stopką strony), dane są zwarte (wiersze 6 px, odstęp w siatce kontrolek 6 px). Nie rozrzedzamy danych dla „powietrza” ani nie zagęszczamy siatki zdjęć dla „wydajności”.

## Elevation & Depth

System jest płaski. Głębię w spoczynku niesie wyłącznie linia `line` i podkład `well`. Cień istnieje tylko na warstwach, które nakładają się na treść (menu rozwijane, panel boczny), i jest długi, miękki, z ujemnym rozproszeniem — raczej sugestia uniesienia niż obrys. Zdjęcia, karty, pola i przyciski nigdy nie mają cienia. Pod panelem bocznym leży zaciemnienie `rgb(0 0 0 / 0.25)`.

### Shadow Vocabulary
- **Menu rozwijane** (`box-shadow: 0 12px 32px -16px rgb(17 17 17 / 0.3)`): popover filtrów i sortowania.
- **Menu małe** (`box-shadow: 0 8px 24px -12px rgb(17 17 17 / 0.25)`): przełącznik języka.
- **Panel boczny** (`box-shadow: -24px 0 48px -32px rgb(17 17 17 / 0.35)`): szuflada zestawienia, cień rzucany w lewo.

### Named Rules
**The Płasko-w-spoczynku Rule.** Element osadzony w stronie nie ma cienia. Cień dostaje tylko warstwa nałożona na stronę.

## Shapes

Kwadratowy świat: wszystkie tokeny promienia (`--radius-sm` … `--radius-xl`, `--radius`) wynoszą 0 px, więc nawet komponenty shadcn/ui renderują się z ostrymi narożnikami. Obrys to 1 px (`line` w spoczynku, `ink` aktywnie); grubszego obrysu nie ma poza 2 px pierścieniem fokusu z odstępem 2 px. Zdjęcia zawsze w proporcji 2:3, przycięte `object-cover`, bez ramek. Jedyny kształt okrągły to kropka stanu „w zestawieniu” (6–10 px, pełne koło `ink`, przy próbce z 2 px obwódką `paper`).

### Named Rules
**The Kwadrat-i-kropka Rule.** Narożniki są ostre bez wyjątku; okrąg zarezerwowany jest wyłącznie dla kropki stanu.

## Components

### Buttons
Płaskie i tekstowe; czerń dla jednej akcji, tekst dla reszty.
- **Shape:** ostre narożniki (0 px), bez obrysu.
- **Primary:** wypełnienie `ink`, tekst `paper`, Small 500, wysokość 48 px, padding poziomy 24 px, zwykle na pełną szerokość kolumny lub panelu („Dodaj do zestawienia”, „Dalej: utwórz PDF”, „Pobierz PDF”). Jedna na widok.
- **Hover / Focus:** tło przechodzi do `ink-2` (transition-colors); fokus to globalny obrys 2 px `ink` z odstępem 2 px. Wyłączona bez zmian w danych: `well` + `ink-3`. Potwierdzenie po akcji zostaje czarne z ikoną check (zoom-in 300 ms). Wysyłanie: `ink-2` z kursorem `wait`.
- **Tekstowy:** Small w `ink-2` lub `ink`, wysokość 40 px, padding 12 px; hover do `ink` albo pojawienie się podkreślenia (offset 4 px, grubość 1 px). Linki pomocnicze w Meta są podkreślone w spoczynku i tracą podkreślenie przy hover.
- **Ikona:** kwadrat 32–36 px, ikona Lucide 14–16 px w `ink-3`/`ink-2`, hover do `ink`.

### Chips
- **Style:** filtr aktywny jako prostokąt 32 px, obrys `line`, Meta, ikona X 14 px; hover obrysu do `ink`.
- **State:** usuwa filtr kliknięciem. Zapytanie wyszukiwania wyświetla się jako ten sam prostokąt bez X.

### Cards / Containers
- **Corner Style:** 0 px.
- **Background:** karta produktu nie ma tła ani ramki — to zdjęcie 2:3 na `well` i blok tekstu z odstępem 12 px pod nim. Bloki pomocnicze (kontakt z opiekunem) to płaszczyzna `well` z paddingiem 20 px.
- **Shadow Strategy:** brak (patrz Elevation & Depth).
- **Border:** brak na karcie; sekcje danych odcinane liniami góra/dół.
- **Internal Padding:** 20 px (blok `well`), 24 px (panele).
- **Hover karty:** zdjęcie powiększa się do 1.025 przez 700 ms `ease-out-expo`, nazwa dostaje podkreślenie.

### Inputs / Fields
- **Style:** obrys 1 px `line`, tło przezroczyste, 0 px, wysokość 44 px, padding 12 px, Small; etykieta nad polem w Meta `ink-3` z odstępem 6 px. Pole wyszukiwania w pasku filtrów jest bez obrysu — ikona lupy 16 px i tekst na linii paska.
- **Focus:** obrys przechodzi do `ink`; pole ilości dostaje tło `well`.
- **Error / Disabled:** obrys `danger` po nieudanej walidacji (`:user-invalid`), komunikat w Meta `danger` pod polem. Zgoda to natywny checkbox 16 px z `accent-color: ink`.

### Toggle box (rozmiar)
Kwadratowe pole wyboru z kodem rozmiaru: obrys `line`, wysokość 44 px (karta produktu) lub 40 px (menu filtrów), Data, siatka 6 kolumn (4 w menu) z odstępem 6 px. Hover: obrys `ink`. Wybrany: negatyw (`ink` tło, `paper` tekst), `aria-pressed`. Wzorzec obowiązuje dla każdego wyboru wielu krótkich wartości.

### Próbka koloru
W wierszu danych: kwadrat 10 px z krawędzią `swatch-edge`. Jako przycisk wyboru: 40 px, obrys 1 px `line` (aktywny `ink`), wewnętrzny odstęp 4 px, próbka z krawędzią 10%. Kropka stanu w prawym górnym rogu oznacza kolor obecny w zestawieniu.

### Stepper ilości
Obrys `line` wokół całości, przyciski −/+ 32–36 px z ikonami 14 px w `ink-2`, pole liczby 44–48 px szerokości oddzielone liniami pionowymi, Data wyśrodkowana. Minus wyłączony przy zerze (`line`). Puste pole oznacza „bez ilości”, nie zero.

### Wiersz danych (lista definicji / tabela)
Etykieta w `ink-3`, wartość w `ink` wyrównana do prawej, Data, odstęp wierszy 4–6 px; grupa odcięta liniami góra/dół z paddingiem 16 px. Tabela: nagłówki kolumn w Meta `ink-3` waga 400, wiersze rozdzielone linią górną `line`, komórki 6 px pionowo, bez pionowych linii i bez tła nagłówka. To podstawowy wzorzec dla gęstych widoków panelu.

### Licznik
Prostokąt min. 20 × 20 px, Meta, Data. Z wartością: negatyw `ink`/`paper` (pojawia się z zoom-in 300 ms). Zero: obrys `line`, tekst dziedziczony (`ink-2` w pasku).

### Menu rozwijane
Panel 0 px, obrys `line`, cień „Menu rozwijane”, szerokość 288 px, odstęp 6 px od wyzwalacza. Pozycje: Small, padding 8 × 12 px, hover `well`, zaznaczenie ikoną check 16 px po prawej (niewidoczna, ale rezerwująca miejsce, gdy niewybrana). Wyzwalacz: tekst + chevron 14 px w `ink-3`; otwarty dostaje podkreślenie.

### Navigation
Linki kategorii w Small `ink-2`, odstęp 24 px, pełna wysokość paska. Hover: `ink`. Aktywny: `ink` + linia 1 px `ink` u dołu, rozwijana od lewej (`scale-x`, 300 ms `ease-out-expo`). Paginacja: tekst „poprzednia / strona n z m / następna” nad linią górną, bez numerów stron w kafelkach.

### Panel boczny (szuflada)
Wysuwany z prawej, pełna szerokość na mobile, maks. 480 px od `sm`. Nagłówek 64 px z linią dolną, tytuł Body 500 z licznikiem `ink-3`; treść przewijana; stopka z podsumowaniem (wiersz danych) i akcją główną, odcięta linią. Kroki (lista → formularz → gotowe) zmieniają treść panelu zamiast otwierać kolejne okna.

### Pas noty
Pełnoszeroki pas `well` z tekstem Meta `ink-2` wyśrodkowanym, padding 8 px. Dla komunikatów systemowych o całej stronie (dane przykładowe); nie dla promocji.

## Do's and Don'ts

### Do:
- **Do** trzymaj każdy element interfejsu w skali `paper` / `well` / `line` / `ink-3` / `ink-2` / `ink`; kolor wnoszą tylko zdjęcia i próbki produktu.
- **Do** oznaczaj wybór negatywem (`ink` tło, `paper` tekst) i obrysem `ink`, a stan „już dodane” kropką `ink`.
- **Do** dawaj `.tabular` każdej liczbie handlowej i wyrównuj kwoty w kolumnach do prawej.
- **Do** oddzielaj grupy danych linią 1 px `line`, nie ramką ani tłem.
- **Do** używaj jednej czarnej akcji głównej 48 px na widok; pozostałe akcje jako tekst lub tekst z podkreśleniem.
- **Do** pokazuj zdjęcia w 2:3 na podkładzie `well`, bez zaokrągleń, ramek i cieni.
- **Do** używaj ikon liniowych Lucide z `strokeWidth` 1.5, 14–16 px, zawsze obok tekstu lub z `aria-label`.
- **Do** animuj krótko i z `ease-out-expo` (`cubic-bezier(0.16, 1, 0.3, 1)`), 300 ms dla stanów, 700 ms dla zdjęć; honoruj `prefers-reduced-motion`.

### Don't:
- **Don't** dodawaj koloru akcentu do interfejsu ani nie koloruj stanów (sukces, nowość, promocja) — wyjątek to `danger` przy błędzie.
- **Don't** zaokrąglaj narożników; okrąg jest tylko kropką stanu.
- **Don't** dawaj cienia elementom osadzonym w stronie (karty, pola, przyciski, zdjęcia).
- **Don't** używaj wag powyżej 500 ani drugiego kroju; logo nie jest krojem i nie odtwarzamy go fontem.
- **Don't** obramowuj każdej komórki tabeli ani nie dawaj nagłówkom tła — to wygląd arkusza, który system odrzuca.
- **Don't** wprowadzaj plakietek „nowość/-30%”, banerów promocji ani metafory koszyka.
