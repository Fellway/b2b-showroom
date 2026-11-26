# Architektura

Uzupełnienie README: tu jest **jak części współpracują i dlaczego**. Tabela endpointów i konfiguracja —
[`backend/README.md`](../backend/README.md), frontend — [`frontend/README.md`](../frontend/README.md).

## Widok całości

```
przeglądarka kupca / admina
        │  https://domena  (jeden host, reverse proxy)
        ├── /            → Next.js 16 (frontend/)  ── katalog publiczny /[locale], panel /admin
        └── /api/...     → Spring Boot 3.5 (backend/) ── REST /api/v1, JWT dla /api/v1/admin/** i /api/v1/buyer/**
                                   │
                                   ├── PostgreSQL 18 (Flyway V1–V3)
                                   ├── SMTP (tylko e-maile transakcyjne kont kupców, D23)
                                   └── FileStorage: dysk lokalny (docelowo S3) — zdjęcia, PDF-y
```

Lokalnie rolę proxy pełni rewrite `/api/:path* → API_INTERNAL_URL` w `frontend/next.config.ts`.
Server Components Next.js czytają katalog bezpośrednio z `API_INTERNAL_URL`; przeglądarka zawsze woła względne `/api/...`.

## Backend — moduły i warstwy

| Moduł | Odpowiada za |
|---|---|
| `catalog` | produkty (agregat: produkt → kolory → warianty SKU/EAN → zdjęcia), kategorie, kolory, rozmiary; widoki katalogu publicznego i panelu |
| `linesheet` | zestawienia (line sheets): migawki pozycji, numeracja `LS-RRRR-NNNNNN`, generowanie i przechowywanie PDF |
| `analytics` | zdarzenia (append-only) i podsumowania/rankingi |
| `contact` | opiekunowie handlowi |
| `identity` | konta panelu i konta kupców (D23): rejestracja, potwierdzenie adresu, akceptacja przez markę, logowanie, rotowane sesje, blokady, reset hasła, e-maile transakcyjne, rejestr zgód (D25), eksport danych i usunięcie konta |
| `legal` | wersjonowane dokumenty prawne — regulamin i polityka prywatności w 4 językach: szkic → publikacja (niezmienna), wymagania wobec kupców (D25) |
| `shared` | jądro: języki, ceny, UUIDv7, hierarchia `DomainException`, porty plików i transakcji, wspólne adaptery web (błędy, język, media, rate limiting) |
| `config` | konfiguracja bezpieczeństwa i zegara |

Każdy moduł: `domain` (czysta Java) → `application` (`port/in`, `port/out`, `service`, `view`) → `adapter`
(`in/web`, `out/persistence`, `out/pdf`, `out/security`…). Moduły rozmawiają tylko przez `application.port.in` /
`application.view` / `domain` innego modułu — pilnuje tego `HexagonalArchitectureTest`.

Kluczowe zasady persystencji (szczegóły w `CLAUDE.md`): wersjonowanie optymistyczne (`version` w żądaniach panelu),
synchronizacja kolekcji dzieci po id, FK do `product_color` odroczone (`DEFERRABLE INITIALLY DEFERRED`) i bez kaskad —
dzieci agregatu usuwa sam agregat.

## Model danych (skrót)

- `category` (+`_translation`, drzewo przez `parent_id`), `color` (+`_translation`), `size`
- `product` (+`_translation`) → `product_color` (PK produkt+kolor) → `product_variant` (SKU, EAN unikalne globalnie), `product_image` (opcjonalnie przypięte do koloru złożonym FK)
- `line_sheet` (migawka danych kupca, właściciel `buyer_account_id` → NULL po usunięciu konta, `public_id` UUIDv4, ścieżka PDF) → `line_sheet_item` (migawka: nazwy, EAN, ceny, zdjęcie; po usunięciu produktu id → NULL, dane zostają)
- `sales_representative`, `analytics_event` (bez FK do katalogu, bez IP), `admin_user`, `admin_refresh_token`
- `buyer_account` (firma, kraj + numer podatkowy, kontakt, język, status, daty zgód, decyzja marki) → `buyer_refresh_token`, `buyer_action_token` (jednorazowe tokeny z e-maili; oba usuwane kaskadowo z kontem)

## Kluczowe przepływy

### Konto kupca (D23)
1. `POST /api/v1/buyer/auth/register` → konto `PENDING_EMAIL`, po commicie e-mail z linkiem `/{lang}/account/verify-email?token=…` (48 h). Zajęty adres: ta sama odpowiedź `202`, a właściciel dostaje e-mail „konto już istnieje”.
2. `POST …/verify-email {token}` → `PENDING_APPROVAL`, e-mail do zespołu marki (wszystkie aktywne konta panelu) z linkiem do `/admin/buyers`.
3. Marka w panelu: `approve` → `ACTIVE` + e-mail do kupca; `reject` → `REJECTED` (+ e-mail, notatka wewnętrzna); później `suspend` (kończy sesje) / `reactivate`, `DELETE` (RODO).
4. `POST …/login` → access token (rola `BUYER`, 15 min) + ciasteczko `showroom_buyer_refresh` (30 dni); nieaktywne konto → `403 identity.buyer.not-active` + `status` (tylko przy poprawnym haśle).
5. Reset hasła: `password-reset/request` (zawsze `202`) → e-mail z linkiem (1 h) → `password-reset/confirm` kończy wszystkie sesje (i potwierdza adres, jeśli nie był potwierdzony).

### Ceny w katalogu (D23)
Publiczne endpointy katalogu nie zwracają cen ani EAN, więc strony katalogu Next.js zostają współdzielone i cache'owane
(Server Components, 60 s). Przeglądarka zalogowanego kupca dociąga je osobno: `GET /api/v1/buyer/catalog/pricing?product=…`
(maks. 100 produktów, `Cache-Control: private, no-store`) i scala z widokiem po `productId` / `variantId`.

### Zestawienie PDF (kupiec)
1. Frontend trzyma wybór w localStorage (`src/store/selection.ts`, maks. 200 pozycji).
2. `POST /api/v1/buyer/line-sheets?lang=…` (token kupca) z listą `variantId` + opcjonalną ilością, wiadomością i `visitorId` (tylko po zgodzie na analitykę). Danych kontaktowych nie przesyłamy — PDF zawiera migawkę danych konta (zgody złożone przy rejestracji).
3. Backend sprawdza, czy konto jest aktywne (access token żyje do 15 min po zawieszeniu), pobiera migawki wariantów z katalogu (tylko aktywne, opublikowane, w widocznej kategorii), nadaje numer, renderuje PDF (Thymeleaf → OpenHTMLtoPDF), zapisuje plik (`line-sheets/RRRR/{publicId}.pdf`, usuwany przy rollbacku), zapisuje zestawienie z właścicielem, po commicie rejestruje zdarzenie `LINE_SHEET_GENERATED`.
4. Odpowiedź `201 {id: publicId, referenceNumber, pdfUrl}`; PDF pod `GET /api/v1/buyer/line-sheets/{publicId}/pdf` — tylko właściciel, żądanie z tokenem (frontend: fetch → blob); cudze zestawienie = `404`. Panel: `GET /api/v1/admin/line-sheets/{id}/pdf`.
5. Rate limit: 5/min i 30/h na adres.

### Logowanie i sesja panelu
1. `POST /api/v1/admin/auth/login` (+ `X-Requested-With`) → access token w body, refresh w ciasteczku.
2. Frontend (`src/lib/admin/session.ts`) trzyma token w pamięci; po przeładowaniu odtwarza sesję przez `POST …/refresh`. Odświeżanie jest single-flight; klient API ponawia żądanie raz po 401.
3. Rotacja przy każdym refresh; ponowne użycie zużytego tokenu → unieważnienie rodziny. Blokada konta: 5 nieudanych prób / 15 min.

### Zdjęcia produktu
Upload multipart → format rozpoznawany po magic bytes (JPEG/PNG/WebP/AVIF, maks. 10 MB) → klucz generuje serwer
(`products/{productId}/{imageId}.{ext}`) → `FileStorage`. Publicznie serwowane wyłącznie `products/**` pod
`/api/v1/media/…` z `Cache-Control: immutable`. PDF-y z danymi kupców nigdy nie idą przez endpoint mediów.

### Analityka i zgoda
`src/lib/analytics.ts` wysyła paczki zdarzeń (do 50, czas nadaje serwer). Bez zgody (`src/lib/consent.ts`) — bez
identyfikatorów i bez zapisu w przeglądarce; po zgodzie `visitorId` + `sessionId`. Dashboard
(`GET /api/v1/admin/analytics/summary`) liczy unikalnych odwiedzających z `visitorId`, rankingi produktów z etykietami
z katalogu (usunięte produkty jako „nieznane”).

### Języki
Katalog: `pl` bez prefiksu, `/en`, `/cs`, `/uk` (next-intl, `src/proxy.ts`); teksty w `frontend/messages/*.json`.
Backend: `?lang=` → `Accept-Language` → `pl`. PDF w języku zestawienia. Panel tylko po polsku.

## Frontend — mapa

| Ścieżka | Zawartość |
|---|---|
| `src/app/[locale]/` | katalog (`page.tsx`), karta produktu (`p/[slug]`), konto kupca (`account/…`), layout z nagłówkiem, stopką, szufladą zestawienia, banerem zgody |
| `src/app/admin/` | logowanie, `(panel)/…` — modele, edytor, słowniki, zestawienia, statystyki, opiekunowie, konta |
| `src/components/{catalog,selection,site,account}` | komponenty katalogu i kont kupców (`catalog/pricing.tsx` — ceny po zalogowaniu) |
| `src/components/admin/{ui,shell,product-list,product-editor,dictionaries,line-sheets,buyers,analytics,representatives,users}` | komponenty panelu (`ui/` — wspólne) |
| `src/lib/` | API katalogu (server-only), formatowanie, analityka, zgoda, typy; `lib/admin/` — sesja, klient API, błędy, zapytania; `lib/buyer/` — sesja i klient API kupca, ceny |
| `src/app/globals.css` | tokeny systemu wizualnego (opis: `DESIGN.md`) |

## Testy

Backend: 258 testów — domena i serwisy (fake'i w pamięci), persystencja na PostgreSQL 18 w Testcontainers
(`@PersistenceTest`), WebMvc, bezpieczeństwo, rate limiting, PDF, end-to-end (`ShowroomEndToEndTest`) i ArchUnit.
Frontend: TypeScript + ESLint + build; weryfikacja wizualna przez Playwright (zob. [development.md](development.md)).
