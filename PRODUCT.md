# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Ustalony przez użytkownika przed init (zob. `CLAUDE.md`): Next.js (App Router, TypeScript), Tailwind CSS, shadcn/ui,
next-intl; backend Spring Boot (REST `/api/v1`) już istnieje. Frontend i API na tej samej domenie za reverse proxy.

## Users

**Kupcy hurtowi (B2B)** sklepów i butików z odzieżą damską — decydują, które modele i rozmiary zamówić do sklepu.
Trafiają do katalogu w czterech sytuacjach, wszystkie potwierdzone:

- **Link od handlowca** — po rozmowie, mailu lub targach przeglądają kolekcję na spokojnie (zwykle laptop).
- **Spotkanie / showroom** — handlowiec pokazuje kolekcję na tablecie lub ekranie podczas spotkania.
- **Nowy kupiec szukający dostawcy** — trafia z wyszukiwarki, Instagrama lub polecenia i dopiero poznaje markę.
- **Stały klient** — zna markę, wraca co sezon, chce szybko przejrzeć nowości i złożyć zestawienie.

**Administrator / menedżer marki** — prowadzi katalog w panelu: produkty, warianty, zdjęcia, tłumaczenia, ukrywanie
niedostępnych modeli, podgląd zestawień i statystyk.

## Product Purpose

Interaktywna wizytówka i katalog B2B marki **Fabian's Style**. Kupiec przegląda kolekcję (lookbook), zaznacza
interesujące warianty (model × kolor × rozmiar) i pobiera spersonalizowane zestawienie PDF z kodami EAN, zdjęciami,
cenami hurtowymi i sugerowanymi cenami detalicznymi, a potem kontaktuje się z opiekunem handlowym.

To nie jest sklep: bez koszyka, płatności i stanów magazynowych. Sukces = kupiec szybko rozumie ofertę, buduje
zestawienie i nawiązuje kontakt handlowy; marka widzi, co kupców interesuje (oglądane, wybierane i zestawiane modele).

## Positioning

Potwierdzone wyróżniki marki — tylko te, bez dopowiadania szczegółów:

- **Produkcja w Polsce.**
- **Autorski design.**
- **Atrakcyjna marża** dla sklepów (relacja ceny hurtowej do sugerowanej ceny detalicznej).

## Operating Context

- Rynki: **Polska jako rynek główny**, rozwój eksportu do **Czech i Ukrainy**; angielski dla pozostałych kupców.
- Ton wobec kupców: **profesjonalny, bezpośredni**.
- Języki interfejsu i treści: pl (bazowy, fallback), en, cs, uk.
- Ceny w PLN: hurtowa netto + sugerowana detaliczna (MSRP/RRP) brutto, na poziomie modelu.
- Struktura oferty: model → kolor → rozmiar → SKU/EAN.
- Zestawienie (line sheet) to dokument PDF do pobrania: kompaktowa tabela, wiersz na wariant z miniaturą.
- Kontakt z opiekunem handlowym przez widoczny e-mail i telefon. Aplikacja nie wysyła e-maili.
- Urządzenia: laptop (link), tablet (spotkanie), telefon (nowi kupcy z social media).

## Capabilities and Constraints

- Publiczny katalog bez logowania: lista z filtrami (kategoria, kolor, rozmiar, wyszukiwanie, wyróżnione), karta
  produktu, wybór wariantów z opcjonalną ilością orientacyjną, generowanie i pobieranie PDF (1–200 wariantów).
- Dane kontaktowe kupca w zestawieniu są opcjonalne i wymagają zgody RODO.
- Panel administracyjny (role ADMIN i EDITOR): CRUD produktów, kolorów, rozmiarów, kategorii, zdjęć, tłumaczeń,
  opiekunów handlowych; statusy produktu (szkic, opublikowany, ukryty, zarchiwizowany); lista zestawień; analityka.
- Panel administracyjny: jedna osoba (właściciel / menedżer marki), praca kilka razy w tygodniu; najczęstsze zadania:
  nowe modele ze zdjęciami, ceny i ukrywanie, przegląd zestawień (leady), statystyki. Tłumaczenia EN/CS/UK przychodzą
  z zewnątrz i są wklejane — panel ma pokazywać braki tłumaczeń.
- Urządzenia: panel w pierwszym etapie tylko na desktop; **docelowo całość (katalog i panel) ma działać mobilnie,
  planowana jest też aplikacja mobilna** — logika wyłącznie w REST API, układy możliwe do przeskładania.
- Zgoda na analitykę (decyzja 2026-10-09): baner prywatności z równorzędnymi „Akceptuję / Odrzucam”, nieblokujący,
  ponownie dostępny z linku „Ustawienia prywatności” w stopce; ważny 12 miesięcy. Bez zgody zdarzenia są w pełni
  anonimowe (nic nie jest zapisywane w przeglądarce); po zgodzie trwały identyfikator odwiedzającego pozwala liczyć
  unikalnych odwiedzających. **Otwarte:** strona polityki prywatności (wymaga danych administratora danych od marki).
- Analityka bez danych osobowych (bez IP), odwiedziny, oglądane / wybierane / zestawiane produkty.
- Limit generowania zestawień (rate limiting) — UI musi obsłużyć komunikat „spróbuj za chwilę”.
- Błędy API mają stabilne kody (`code`), które frontend tłumaczy na komunikaty w 4 językach.
- **Otwarte:** dostęp kupców przez token w URL (świadomie odłożony); identyfikacja wizualna marki w PDF (dziś
  placeholdery, do przeniesienia z ustaleń wizualnych).

## Brand Commitments

- Nazwa marki: **Fabian's Style** (zapis z logo i banera: wielkie litery, apostrof). Logo kaligraficzne — używać
  wyłącznie dostarczonego pliku, nie odtwarzać fontem.
- Istniejący kanał hurtowy: grupa na Facebooku „Klienci hurtowi Fabian's Style”.
- Kanały: [Facebook](https://www.facebook.com/fabiansstyle) i [Instagram](https://www.instagram.com/fabiansstyle/)
  (bez logowania nie udostępniają treści — informacje o marce pochodzą wyłącznie od użytkownika).
- Asortyment: odzież damska.
- Ton: profesjonalny, bezpośredni.
- **Kierunek wizualny (decyzja 2026-10-06):** standard kategorii — dopracowany katalog hurtowy bez eksperymentów
  formalnych, wykonany na poziomie **COS / Arket** (poprzeczka jakości: duże zdjęcia, dużo powietrza, precyzyjna
  typografia). Kupiec ma czuć się jak w znajomym narzędziu, nie w sklepie detalicznym ani w arkuszu kalkulacyjnym.

## Evidence on Hand

Dostarczone do `brand-assets/` (katalog poza gitem — zdjęcia i dane marki nie trafiają do repozytorium):

- `logo.png` — logo kaligraficzne (pismo odręczne, czarne na białym), 936×936 px, tylko raster bez przezroczystości;
  brak wersji wektorowej (SVG) — do uzyskania od marki.
- `facebook-zdj-w-tle.png` — baner Facebooka (820×360): modelka w czarnej sukience, logo, odnośniki do
  **grupy hurtowej na Facebooku „Klienci hurtowi Fabian's Style”** i profilu @fabiansstyle.
- `zdj-sesja/` — 13 zdjęć z sesji studyjnych (11 × 1100×1650 px, 2 × 393×590 px): jasny loft z białą cegłą i dużym
  oknem, drewniana boazeria, biała zasłona, kremowe meble bouclé, rośliny. Modelki w sukienkach midi i sukienkach
  koszulowych z wyrazistymi printami (abstrakcyjne pociągnięcia pędzla, duże kwiaty) — m.in. brąz, kobalt, czerń,
  zieleń, bordo z akcentami złota.
- `modelki-ai/` — 10 zdjęć **wygenerowanych przez AI** (modelki w miejskim, jesiennym plenerze, 688–896 px szerokości).
  **Decyzja marki (2026-10-06):** zdjęcia AI są używane na równi ze zdjęciami z sesji, także przy produktach.

Zapowiedziane, ale jeszcze niedostarczone: packshoty produktów, dane produktów (nazwy, składy, rozmiarówki, EAN, ceny)
— do czasu ich dostarczenia katalog działa na danych demonstracyjnych oznaczonych jako przykładowe.

Brak: opinii klientów, case studies, danych o sprzedaży, liczby sklepów partnerskich, nagród, certyfikatów materiałowych.
Niczego z tej listy nie wolno wymyślać w treściach.

## Product Principles

1. **Kupiec decyduje szybko.** Model, cena hurtowa, MSRP, rozmiary i EAN są zawsze o jeden krok — bez logowania,
   bez barier, bez marketingowego szumu między kupcem a ofertą.
2. **Kolekcja prowadzi, interfejs służy.** Autorski design i zdjęcia lookbookowe są głównym argumentem; interfejs
   nie konkuruje z produktem.
3. **Liczby to fakty handlowe.** Ceny, EAN, rozmiary i kolory są dokładne i identyczne na ekranie i w PDF.
4. **Jeden katalog, cztery sytuacje.** Ten sam katalog działa przy spokojnym przeglądaniu linku, na spotkaniu
   z handlowcem, dla nowego kupca z Instagrama i dla stałego klienta, który wraca po nowy sezon.
5. **Polska najpierw, gotowość na eksport.** Polski jest językiem bazowym; czeski, ukraiński i angielski są pełnoprawne,
   nie są dodatkiem.
