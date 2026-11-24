# Fabian's Style — frontend katalogu B2B

Next.js 16 (App Router, TypeScript) · Tailwind CSS 4 · shadcn/ui (Radix) · next-intl 4 · zustand

Publiczny katalog hurtowy: lista kolekcji z filtrami, karta produktu z wyborem wariantów, zestawienie (line sheet)
z generowaniem PDF i kontakt z opiekunem handlowym oraz panel administracyjny pod `/admin`.

## Panel administracyjny (`/admin`)

- Osobny layout po polsku, poza routingiem językowym (`src/proxy.ts` pomija `/admin`), `noindex`.
- Sesja (`src/lib/admin/session.ts`): access token JWT tylko w pamięci karty, refresh token w ciasteczku HttpOnly;
  odświeżanie jest single-flight (dwa równoległe refresh-e backend traktuje jak kradzież sesji). Klient API
  (`src/lib/admin/api.ts`) ponawia żądanie raz po 401. Dane: TanStack Query; błędy po kodach (`src/lib/admin/errors.ts`).
- Ekrany: Modele (lista z szybką ceną i ukrywaniem, kolumna „Braki”), edytor modelu w krokach (dane, tłumaczenia
  wklejane obok polskiego wzorca, kolory × rozmiary z SKU/EAN, zdjęcia), Kategorie / Kolory / Rozmiary, Zestawienia
  (leady z kontaktem i PDF), Statystyki, Opiekunowie, Konta (tylko ADMIN).
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
| `NEXT_PUBLIC_PRIVACY_POLICY_URL` | adres polityki prywatności marki; pusty = brak linku w banerze zgody i stopce |

## Jak to działa

- **Ta sama domena co API.** Przeglądarka woła zawsze względne `/api/...`; lokalnie przekierowuje je rewrite
  w `next.config.ts`, w produkcji reverse proxy. Brak CORS, zdjęcia z `/api/v1/media/products/**` przez `next/image`.
- **Języki:** `pl` bez prefiksu (rynek główny), `/en`, `/cs`, `/uk`. Teksty w `messages/*.json` (ICU, poprawne
  liczby mnogie). Błędy API tłumaczone po stabilnym `code` (`src/lib/problem.ts`), nigdy po treści komunikatu.
- **Dane katalogu** pobierają Server Components (`src/lib/api.ts`, odświeżanie co 60 s); filtry żyją w URL,
  więc link z filtrami można przesłać kupcowi.
- **Zestawienie** (`src/store/selection.ts`) to stan po stronie przeglądarki (localStorage, maks. 200 pozycji);
  PDF składa backend z aktualnego katalogu w języku żądania.
- **Analityka** (`src/lib/analytics.ts`) bez ciasteczek: zdarzenia paczkami, tylko identyfikator sesji karty.
  Trwały identyfikator odwiedzającego czeka na decyzję o zgodzie.
- **Kierunek wizualny:** standard kategorii na poziomie COS / Arket — kontrakt w
  `.impeccable/surfaces/`, tokeny w `src/app/globals.css`, system opisany w `DESIGN.md` w katalogu głównym.

## Kontrola jakości

```bash
npx tsc --noEmit && npm run lint && npm run build
```
