# Architektura

Uzupełnienie README: tu jest **jak części współpracują i dlaczego**. Tabela endpointów i konfiguracja —
[`backend/README.md`](../backend/README.md), frontend — [`frontend/README.md`](../frontend/README.md).

## Widok całości

```
przeglądarka kupca / admina
        │  https://domena  (jeden host, reverse proxy)
        ├── /            → Next.js 16 (frontend/)  ── katalog publiczny /[locale], panel /admin
        └── /api/...     → Spring Boot 3.5 (backend/) ── REST /api/v1, JWT dla /api/v1/admin/**
                                   │
                                   ├── PostgreSQL 18 (Flyway V1, V2)
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
| `identity` | konta adminów, logowanie, refresh tokeny, blokady |
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
- `line_sheet` (dane kupca, publiczny `public_id` UUIDv4, ścieżka PDF) → `line_sheet_item` (migawka: nazwy, EAN, ceny, zdjęcie; po usunięciu produktu id → NULL, dane zostają)
- `sales_representative`, `analytics_event` (bez FK do katalogu, bez IP), `admin_user`, `admin_refresh_token`

## Kluczowe przepływy

### Zestawienie PDF (kupiec)
1. Frontend trzyma wybór w localStorage (`src/store/selection.ts`, maks. 200 pozycji).
2. `POST /api/v1/line-sheets?lang=…` z listą `variantId` + opcjonalną ilością, opcjonalnymi danymi kupca (wymagają `privacyAccepted`) i `visitorId` (tylko po zgodzie na analitykę).
3. Backend pobiera migawki wariantów z katalogu (tylko aktywne, opublikowane, w widocznej kategorii), nadaje numer, renderuje PDF (Thymeleaf → OpenHTMLtoPDF), zapisuje plik (`line-sheets/RRRR/{publicId}.pdf`, usuwany przy rollbacku), zapisuje zestawienie, po commicie rejestruje zdarzenie `LINE_SHEET_GENERATED`.
4. Odpowiedź `201 {id: publicId, referenceNumber, pdfUrl}`; PDF pod `GET /api/v1/line-sheets/{publicId}/pdf` (`no-store`, `noindex`).
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
| `src/app/[locale]/` | katalog (`page.tsx`), karta produktu (`p/[slug]`), layout z nagłówkiem, stopką, szufladą zestawienia, banerem zgody |
| `src/app/admin/` | logowanie, `(panel)/…` — modele, edytor, słowniki, zestawienia, statystyki, opiekunowie, konta |
| `src/components/{catalog,selection,site}` | komponenty katalogu |
| `src/components/admin/{ui,shell,product-list,product-editor,dictionaries,line-sheets,analytics,representatives,users}` | komponenty panelu (`ui/` — wspólne) |
| `src/lib/` | API katalogu (server-only), formatowanie, analityka, zgoda, typy; `lib/admin/` — sesja, klient API, błędy, zapytania |
| `src/app/globals.css` | tokeny systemu wizualnego (opis: `DESIGN.md`) |

## Testy

Backend: 258 testów — domena i serwisy (fake'i w pamięci), persystencja na PostgreSQL 18 w Testcontainers
(`@PersistenceTest`), WebMvc, bezpieczeństwo, rate limiting, PDF, end-to-end (`ShowroomEndToEndTest`) i ArchUnit.
Frontend: TypeScript + ESLint + build; weryfikacja wizualna przez Playwright (zob. [development.md](development.md)).
