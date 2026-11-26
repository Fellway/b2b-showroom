# Fabian's Style — frontend katalogu B2B

Next.js 16 (App Router, TypeScript) · Tailwind CSS 4 · shadcn/ui (Radix) · next-intl 4 · zustand

Publiczny katalog hurtowy: lista kolekcji z filtrami, karta produktu z wyborem wariantów, zestawienie (line sheet)
z generowaniem PDF i kontakt z opiekunem handlowym, konta kupców (ceny i EAN po zalogowaniu, D23) oraz panel
administracyjny pod `/admin`.

## Konta kupców (D23)

- Strony (4 języki, `noindex`, renderowane na żądanie): `/account/login`, `/account/register`,
  `/account/verify-email?token=`, `/account/reset-password[?token=]`, `/account` (dane, hasło, „Moje zestawienia”).
  Ścieżki z tokenem są w linkach e-maili backendu — nie zmieniać bez zmiany `TransactionalBuyerMailer`.
- Sesja (`src/lib/buyer/session.ts`): ta sama mechanika co w panelu (token w pamięci, refresh z ciasteczka
  `showroom_buyer_refresh`, single-flight). Znacznik w localStorage (bez danych) mówi, że w tej przeglądarce ktoś
  był zalogowany — tylko wtedy po przeładowaniu odtwarzamy sesję (gość nie wysyła prób odświeżenia).
- Ceny i EAN (`usePricing` w `src/lib/buyer/api.ts`): strony katalogu są wspólne i cache'owane, a przeglądarka
  zalogowanego kupca dociąga `GET /api/v1/buyer/catalog/pricing` — jedno żądanie na stronę (`PricingScope`).
  Pamięć cen czyszczona przy wylogowaniu. Gość widzi „Ceny po zalogowaniu” i pasek z logowaniem/rejestracją.
- Zestawienie: wybór działa także dla gościa (bez cen w localStorage), utworzenie PDF wymaga logowania — logowanie
  z szuflady wraca na tę samą stronę i otwiera zestawienie (`?next=…&selection=1`). PDF pobierany z tokenem
  (fetch → blob), także w panelu.
- Token z linku e-mail jest wysyłany najwyżej raz i usuwany z paska adresu po hydratacji (`components/account/token-link.ts`).
- Telefon (D24): kraj z flagą i numerem kierunkowym + numer krajowy (`components/account/phone-field.tsx`,
  `lib/phone.ts`), zapis `+48 600 111 222`. Rejestracja podpowiada kraj z żądania (`lib/request-country.ts`:
  nagłówek proxy `CF-IPCountry` / `X-Country-Code` → `Accept-Language` → język strony).
- „Moje konto” po otwarciu pobiera `/api/v1/buyer/me` — dane konta w sesji są z chwili logowania i mają nieaktualną
  wersję (logowanie zapisuje czas logowania), a edycja wymaga bieżącej.
- Dokumenty prawne (D25): strony `/terms` i `/privacy` (obowiązująca wersja, `?version=<id>` — archiwalna, historia
  wersji) renderowane z ograniczonego Markdownu bez HTML-a (`components/legal/legal-markdown.tsx`). Rejestracja wysyła
  id wersji pokazanych w formularzu (pobranych bez cache). Stan zgód kupca: `lib/buyer/legal.ts` — po zalogowaniu
  i po odpowiedzi `identity.buyer.terms-acceptance-required` z API. Nowy regulamin = okno bez możliwości pominięcia
  (`components/account/legal-notices.tsx`; wyjście: wylogowanie albo „Moje konto”), nowa polityka = pasek pod
  nagłówkiem. Po akceptacji ceny pobierane od nowa (`invalidatePricing`). „Moje konto”: akceptacja w treści strony,
  historia zgód, pobranie danych (JSON), usunięcie konta z hasłem (`components/account/account-privacy.tsx`).

## Panel administracyjny (`/admin`)

- Osobny layout po polsku, poza routingiem językowym (`src/proxy.ts` pomija `/admin`), `noindex`.
- Sesja (`src/lib/admin/session.ts`): access token JWT tylko w pamięci karty, refresh token w ciasteczku HttpOnly;
  odświeżanie jest single-flight (dwa równoległe refresh-e backend traktuje jak kradzież sesji). Klient API
  (`src/lib/admin/api.ts`) ponawia żądanie raz po 401. Dane: TanStack Query; błędy po kodach (`src/lib/admin/errors.ts`).
- Ekrany: Modele (lista z szybką ceną i ukrywaniem, kolumna „Braki”), edytor modelu w krokach (dane, tłumaczenia
  wklejane obok polskiego wzorca, kolory × rozmiary z SKU/EAN, zdjęcia), Kategorie / Kolory / Rozmiary, Zestawienia
  (leady z kontaktem i PDF), Kupcy (wnioski do akceptacji z licznikiem w nawigacji, akceptacja / odrzucenie /
  zawieszenie / usunięcie konta, rejestr zgód), Statystyki, Opiekunowie, Dokumenty prawne (szkic w 4 językach
  z podglądem → publikacja wersji, tylko ADMIN; `?edit=<id>`), Konta (tylko ADMIN).
- Wspólne komponenty panelu: `src/components/admin/ui/` (pola, tabela, nagłówek strony, panel boczny z formularzem,
  statusy, potwierdzenie, komunikat „Zapisano”).

## Uruchomienie

```bash
nvm use                      # Node 22 (.nvmrc)
npm ci
cp .env.example .env.local   # albo ustaw zmienne ręcznie
npm run dev                  # http://localhost:3000 — wymaga backendu na API_INTERNAL_URL
```

Backend z danymi demonstracyjnymi: `scripts/dev-up.sh --seed` w katalogu głównym (zob. `backend/README.md`).
Na Windowsie: `scripts\dev-up.ps1 -Seed -Frontend` stawia bazę, backend, dane demo i frontend naraz.

| Zmienna | Opis |
|---|---|
| `API_INTERNAL_URL` | adres Spring Boot widziany z serwera Next.js (domyślnie `http://localhost:8080`) |
| `NEXT_PUBLIC_DEMO_NOTICE` | `true` pokazuje pasek „katalog demonstracyjny” — dopóki dane są przykładowe |

## Jak to działa

- **Ta sama domena co API.** Przeglądarka woła zawsze względne `/api/...`; lokalnie przekierowuje je rewrite
  w `next.config.ts`, w produkcji reverse proxy. Brak CORS, zdjęcia z `/api/v1/media/products/**` przez `next/image`.
- **Języki:** `pl` bez prefiksu (rynek główny), `/en`, `/cs`, `/uk`. Teksty w `messages/*.json` (ICU, poprawne
  liczby mnogie). Błędy API tłumaczone po stabilnym `code` (`src/lib/problem.ts`), nigdy po treści komunikatu.
- **Dane katalogu** pobierają Server Components (`src/lib/api.ts`, odświeżanie co 60 s); filtry żyją w URL,
  więc link z filtrami można przesłać kupcowi.
- **Zestawienie** (`src/store/selection.ts`) to stan po stronie przeglądarki (localStorage, maks. 200 pozycji,
  bez cen i EAN — od wersji 2 magazynu); PDF składa backend z aktualnego katalogu i danych konta w języku żądania.
- **Nagłówek na stronach statycznych:** komponenty z `useSearchParams` muszą być w granicy `Suspense`
  (`CategoryNav`) albo czytać adres w obsłudze zdarzenia (`LocaleSwitcher`) — inaczej prerender kończy się błędem.
- **Analityka** (`src/lib/analytics.ts`) bez ciasteczek: zdarzenia paczkami, tylko identyfikator sesji karty.
  Trwały identyfikator odwiedzającego czeka na decyzję o zgodzie.
- **Kierunek wizualny:** standard kategorii na poziomie COS / Arket — kontrakt w
  `.impeccable/surfaces/`, tokeny w `src/app/globals.css`, system opisany w `DESIGN.md` w katalogu głównym.

## Kontrola jakości

```bash
npx tsc --noEmit && npm run lint && npm run build
```
