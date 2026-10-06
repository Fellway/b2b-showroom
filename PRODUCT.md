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

Interaktywna wizytówka i katalog B2B marki **fabian's style**. Kupiec przegląda kolekcję (lookbook), zaznacza
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
- Analityka bez danych osobowych (bez IP), odwiedziny, oglądane / wybierane / zestawiane produkty.
- Limit generowania zestawień (rate limiting) — UI musi obsłużyć komunikat „spróbuj za chwilę”.
- Błędy API mają stabilne kody (`code`), które frontend tłumaczy na komunikaty w 4 językach.
- **Otwarte:** dostęp kupców przez token w URL (świadomie odłożony); identyfikacja wizualna marki w PDF (dziś
  placeholdery, do przeniesienia z ustaleń wizualnych).

## Brand Commitments

- Nazwa marki: **fabian's style** (pisownia z apostrofem; potwierdzić zapis wielkimi/małymi literami z logo).
- Kanały: [Facebook](https://www.facebook.com/fabiansstyle) i [Instagram](https://www.instagram.com/fabiansstyle/)
  (bez logowania nie udostępniają treści — informacje o marce pochodzą wyłącznie od użytkownika).
- Asortyment: odzież damska.
- Ton: profesjonalny, bezpośredni.

## Evidence on Hand

Istnieją (potwierdzone), ale **nie zostały jeszcze dostarczone do repozytorium** — ścieżki do uzupełnienia:

- logo marki,
- sesje lookbookowe (stylizacje na modelkach),
- packshoty produktów,
- opisy i dane produktów (nazwy, składy, rozmiarówki, EAN, ceny).

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
