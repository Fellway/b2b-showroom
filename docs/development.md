# Praca deweloperska

## Wymagania

- Java 21 (Temurin), Docker (Testcontainers i baza dev), Node 22 przez nvm (`frontend/.nvmrc`), Google Chrome (zrzuty ekranu).
- Maven — **tylko przez wrapper** z katalogu `backend/` (`./mvnw`, Maven 3.9.16, Maven Central — zob. decyzja D4).

## Pierwsze uruchomienie

```bash
cp .env.example .env            # uzupełnij: SHOWROOM_JWT_SECRET (openssl rand -base64 48), dane pierwszego admina
scripts/dev-up.sh --build --seed  # baza (kontener showroom-dev-db, port 5433) + backend :8080 + dane demonstracyjne
cd frontend && nvm use && npm ci
cp .env.example .env.local      # NEXT_PUBLIC_DEMO_NOTICE=true dopóki dane są przykładowe
npm run dev                     # http://localhost:3000, panel: /admin (login z .env)
```

`scripts/dev-up.sh stop` zatrzymuje backend i bazę. Log backendu: `backend/data/dev-backend.log`.
Pliki (zdjęcia, PDF-y) lądują w `backend/data/storage` (poza gitem).

### Windows (bez Dockera i WSL)

`scripts/dev-up.ps1` to odpowiednik `dev-up.sh`, tyle że baza jest natywną usługą Windows zamiast kontenera.
Jednorazowo, przez winget:

```powershell
winget install --id OpenJS.NodeJS.22 -e
winget install --id PostgreSQL.PostgreSQL.18 -e --override "--mode unattended --unattendedmodeui none --superpassword <hasło> --serverport 5433 --disable-components stackbuilder,pgAdmin"
# dopisz do .env: POSTGRES_SUPERUSER_PASSWORD=<hasło> — skrypt sam założy rolę i bazę showroom
cd frontend; npm ci             # node_modules zbudowane na Linuksie/macOS nie działają na Windowsie
```

```powershell
scripts\dev-up.ps1 -Build -Seed -Frontend   # baza (usługa postgresql-x64-18, :5433) + backend :8080 + dane demo + frontend :3000
scripts\dev-up.ps1 stop                     # backend i frontend; usługa bazy działa dalej (startuje z systemem)
```

Logi: `backend/data/dev-backend.log`, `frontend/.next-dev.log`. Backend nie wstaje sam po restarcie komputera —
uruchom skrypt ponownie. Wymagana Java ≥ 21 (działa też na Temurin 25).

**Dane demonstracyjne** (`scripts/seed-demo.mjs`, idempotentny, tylko przez REST API): 9 modeli ze zdjęć
w `brand-assets/` (7 opublikowanych, 1 szkic, 1 ukryty), 15 kolorów, rozmiary 36–46, EAN z prefiksem 29
(zakres wewnętrzny GS1 — nigdy nie koliduje z prawdziwymi kodami), przykładowy opiekun „Fabian's Style B2B”
z `hurt@example.com`. Ceny i opisy są ilustracyjne.

`brand-assets/` (logo, zdjęcia marki) jest **poza gitem** — trzymaj kopię poza repozytorium.

## Kontrola jakości

```bash
cd backend && ./mvnw test                                   # 258 testów, wymaga Dockera
cd frontend && npx tsc --noEmit && npm run lint && npx next build
~/.claude/skills/impeccable/scripts/impeccable detect --json frontend/src   # detektor wzorców (wtyczka Impeccable)
```

## Pułapki środowiska (sprawdzone w praktyce)

- **`docker compose up` nie działa na tej maszynie** — Dockerowi skończyły się pule adresów sieci. `dev-up.sh` stawia
  bazę na domyślnej sieci `bridge`. Nie czyścić sieci (`docker network prune`) bez zgody właściciela.
- **zsh:** `pkill -f '<wzorzec>'` zabija też powłokę, jeśli wzorzec występuje w treści polecenia — zatrzymuj procesy
  po PID w osobnym poleceniu. W pętlach zsh nie używaj zmiennej `path` (to alias `PATH`).
- **Firmowy `~/.m2/settings.xml`** wskazuje Artifactory Empiku — dlatego projekt ma własne `.mvn/settings.xml`.
- **Systemowy Node to 16** — zawsze `nvm use` (Node 22). Playwright też wymaga Node ≥ 20.
- **Testy pełnego kontekstu** (`@SpringBootTest`) wymagają właściwości `showroom.security.jwt.secret`
  i `showroom.storage.local-root`; test z wieloma logowaniami ustawia `showroom.rate-limit.enabled=false`.
  Nie tworzyć `src/test/resources/application.yml` (przesłania główny plik).
- **Odroczone FK** (`product_color`) sprawdzane są przy commicie — testy z rollbackiem wymuszają je
  `SET CONSTRAINTS ALL IMMEDIATE`.
- **Next.js 16:** middleware to `src/proxy.ts`; `params`/`searchParams` są asynchroniczne; `images.localPatterns`
  musi obejmować każdą lokalną ścieżkę `next/image` (`/api/v1/media/products/**`, `/brand/**`). Przed pisaniem kodu
  czytaj `frontend/node_modules/next/dist/docs/`. Zmiana `next.config.ts` wymaga restartu serwera dev.
  Typy tras regeneruje `npx next typegen` (np. po dodaniu nowego layoutu).
- **Windows, SWC:** natywny moduł `@swc/core` (ładowany przez plugin next-intl) odrzuca katalog cache
  `%LOCALAPPDATA%\swc`, gdy jego ACL nadaje pełne prawa obcemu SID-owi (np. piaskownicy AppContainer) —
  `next dev` kończy się błędem `ERR_SWC_NATIVE_CACHE`. Obejście bez zmiany uprawnień w profilu:
  `SWC_NATIVE_BINDING_CACHE=frontend/.swc-cache` (ustawia je `dev-up.ps1` i `.claude/launch.json`).
- **Windows, `next build`:** Turbopack wywraca się na `next/font/google` („queries have exactly one entry”) —
  lokalnie buduj `npx next build --webpack`. CI i obraz Dockera (Linux) używają domyślnego Turbopacka.
  Testując lokalnie serwer `standalone`, ustaw `HOSTNAME=0.0.0.0` (jak w obrazie) — przy `127.0.0.1` rewrite
  middleware next-intl wskazuje na `localhost` i strona główna wpada w pętlę przekierowań.
- **Next.js — strony statyczne i `useSearchParams`:** komponent z `useSearchParams` w layoucie bez granicy
  `Suspense` psuje prerender stron statycznych (`missing-suspense-with-csr-bailout`). Strony konta kupca
  renderujemy na żądanie (`await connection()`), żeby nie zamrażać w nich danych z chwili buildu.
- **Ręczne testy kont kupców lokalnie:** e-maile są wyłączone (`SHOWROOM_MAIL_ENABLED=false`) — link
  potwierdzający / resetu jest w `backend/data/dev-backend.log`; wniosek akceptujesz w panelu (`/admin/buyers`).
- **Windows, PowerShell 5.1:** `Get-Content` bez `-Encoding UTF8` wyświetla pliki UTF-8 jako krzaki (sam plik jest
  poprawny).
- **Windows, PowerShell 5.1:** pliki `.ps1` z polskimi znakami muszą być zapisane w UTF-8 **z BOM**,
  inaczej są czytane jako ANSI. Przekierowanie stderr programu natywnego (`2>$null`) przy
  `$ErrorActionPreference = 'Stop'` przerywa skrypt.
- **Dev-only:** React StrictMode podwaja zdarzenia analityczne w trybie deweloperskim — w produkcji tego nie ma.
- **Cache katalogu:** Server Components odświeżają dane co 60 s — zmiana w panelu pojawia się w katalogu z opóźnieniem.

## Weryfikacja wizualna (Playwright)

Skrypty w stylu `playwright-core` + `/usr/bin/google-chrome` (bez pobierania przeglądarek). Zasady:
- czekaj na `waitUntil: "load"` — `networkidle` nie następuje w trybie dev (połączenie HMR);
- emuluj urządzenia przez `viewport` + `isMobile` — Chrome headless z `--window-size` ma minimalną szerokość ~500 px
  i daje fałszywe przepełnienia;
- obrazy `next/image` ładują się leniwie — przewiń stronę przed zrzutem całej strony;
- logowanie do panelu przez formularz (dane z `.env`).
Zrzuty z przeglądów leżą w `.impeccable/review/` (poza gitem).

## Konwencje commitów

Commity po polsku, opisowe, z linią `Co-Authored-By: Claude …` gdy współtworzył je Claude. Jeden etap = jeden
lub kilka commitów z opisem zakresu (zob. `git log`).
