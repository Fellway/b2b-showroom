---
version: 1
slug: "frontend-src-app-locale-page-tsx"
primary_target: "frontend/src/app/[locale]/page.tsx"
related_targets: ["frontend/src/app/[locale]/p/[slug]/page.tsx"]
---

## Scope

Publiczny katalog hurtowy Fabian's Style: lista kolekcji z filtrami, karta produktu, wybór wariantów (zestawienie),
generowanie i pobieranie zestawienia PDF, kontakt z opiekunem handlowym. Tryb: **Persuade** (kupiec decyduje i działa).
Panel admina to osobna powierzchnia (Operate) dziedzicząca ten świat.

## Audience, job, action

Kupiec butiku z odzieżą damską: link od handlowca (laptop), spotkanie (tablet), nowy kupiec z Instagrama (telefon),
stały klient po nowy sezon. Sukces: złożone zestawienie PDF i kontakt z handlowcem. Dowody: wyłącznie realne zdjęcia
marki (sesja + AI na równi, decyzja marki); dane produktów do czasu importu są przykładowe i oznaczone jako przykładowe.
Ryzyka wskazane przez użytkownika: wygląd sklepu detalicznego, wygląd arkusza kalkulacyjnego, interfejs konkurujący z printami.

## Direction contract

THESIS: Standard kategorii wykonany na poziomie COS / Arket — cichy, prowadzony zdjęciem katalog hurtowy, w którym
jedynym kolorem jest kolekcja, a każda liczba potrzebna kupcowi (hurt, MSRP, mnożnik, rozmiary) jest o jedno spojrzenie.
Odrzuca chrom sklepu B2C (banery promocji, koszyk, plakietki „nowość/-30%”) i gęstość arkusza.

OWN-WORLD: biel jako tło, jasny ciepłoszary #F2F1EF jako podkład zdjęć, czerń logo jako jedyny kolor interfejsu
(tekst, aktywne stany, przyciski), włoskowate linie #E2E0DC. Zdjęcia 2:3 bez zaokrągleń i cieni. Jedna neutralna
groteska z cyrylicą (Golos Text), cyfry tabelaryczne w danych; kaligraficzne logo wyłącznie jako znak marki.
Kontrolki tekstowe i płaskie: czarne wypełnienie dla akcji głównej, podkreślenie dla aktywnego filtra, rozmiary jako
kwadratowe pola z obrysem.

STORY: kupiec od razu widzi kolekcję w dużych zdjęciach, rozumie, że to oferta hurtowa szyta w Polsce (ceny netto,
MSRP, mnożnik), zaznacza rozmiary przy kolorach, widzi rosnące zestawienie, pobiera PDF i dzwoni lub pisze do
opiekuna, którego dane ma pod ręką.

FIRST VIEWPORT: górny pasek — logo po lewej (ok. 28 px wysokości), kategorie jako linki tekstowe, po prawej przełącznik
języka i „Zestawienie (n)”. Pod nim jedna linia kontekstu (kolekcja hurtowa, szyte w Polsce, ceny netto) i rząd
filtrów tekstowych (kategoria, kolor, rozmiar, sortowanie). Od razu siatka zdjęć 2:3 — 4 kolumny na desktopie,
pierwszy rząd w całości widoczny przy 1440×900; pod zdjęciem nazwa, kolory jako małe próbki, hurt i MSRP.
Akcja główna: „Zestawienie (n)” w pasku + „Dodaj do zestawienia” na karcie produktu.

FORM: kanon kategorii (standing exit wybrany przez użytkownika; poprzeczka COS / Arket); seed key 20a4c91b.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## Unresolved

- Identyfikator odwiedzającego dla analityki a zgoda (ePrivacy) — do czasu decyzji tylko identyfikator sesji.
- Prawdziwe dane produktów (import) — dziś dane przykładowe.
