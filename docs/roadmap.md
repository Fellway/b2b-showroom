# Status i plan

## Gdzie jesteśmy (2026-11-26)

| Etap | Zakres | Status |
|---|---|---|
| 1 | Model danych PostgreSQL + Flyway | ✅ |
| 2 | Backend heksagonalny, REST API, JWT | ✅ |
| 3 | PDF zestawień, rezygnacja z e-maili, rate limiting | ✅ |
| 4a | Katalog publiczny (Next.js, 4 języki, zestawienie PDF) | ✅ |
| 4b | Panel administracyjny | ✅ |
| — | Baner zgody na analitykę | ✅ |
| — | Paczki rozmiarowe (D21) | ✅ |
| 5 | Wdrożenie: obrazy Docker, `deploy/` (Caddy + compose + kopie), CI i obrazy w GHCR (D22) | 🟡 przygotowane 2026-11-24, czeka na serwer i domenę |
| 6 | Konta kupców: ceny/EAN/PDF po zalogowaniu, rejestracja z akceptacją, e-maile przez Brevo (D23) | ✅ 2026-11-25 |
| — | Telefon z numerem kierunkowym i podpowiedź kraju (D24) | ✅ 2026-11-25 |
| — | Regulamin i polityka prywatności z wersjami, rejestr zgód, prawa z RODO w „Moim koncie” (D25) | ✅ 2026-11-26 — treść do weryfikacji prawnej |
| — | Wersja mobilna: katalog na telefonie, kluczowe akcje panelu (kupcy, zestawienia, szybkie zmiany modeli), PWA (D26) | ✅ 2026-11-26 |

Aplikacja działa lokalnie end-to-end na danych demonstracyjnych (z kontami kupców i wysyłką e-maili przez Brevo).
CI na GitHubie: testy backendu (z bazą w Testcontainers), typy/lint/build frontendu, obrazy w GHCR z `main`. Repozytorium: [github.com/Fellway/b2b-showroom](https://github.com/Fellway/b2b-showroom).

## Etap 6 — konta kupców (D23), plan

Priorytet biznesowy od 2026-11-24 — przed importem/eksportem Excela.

- **6a. Backend — ✅ 2026-11-24** (migracja V3, API w `backend/README.md`, przepływy w `architecture.md`, D23).
  Konta kupców w module `identity` (rejestracja, potwierdzenie adresu, akceptacja, logowanie z rolą `BUYER`, reset hasła,
  e-maile transakcyjne w 4 językach), ceny i EAN tylko z `/api/v1/buyer/catalog/pricing`, zestawienia i PDF tylko dla
  właściciela, obsługa wniosków w panelu (`/api/v1/admin/buyers`), limity zapytań. Sprawdzone end-to-end na żywym API.
- **6b. Frontend — ✅ 2026-11-25** (szczegóły w `frontend/README.md`). Logowanie, rejestracja, potwierdzenie adresu,
  reset hasła i „Moje konto” z „Moimi zestawieniami” (4 języki); katalog z cenami i EAN po zalogowaniu (dociągane
  w przeglądarce), dla gości pasek z logowaniem; zestawienie wymaga logowania (powrót do szuflady po zalogowaniu),
  PDF przez fetch → blob; panel: sekcja „Kupcy” z licznikiem wniosków. Sprawdzone end-to-end w przeglądarce
  (rejestracja → potwierdzenie → akceptacja → ceny → PDF). Scalone do `main` 2026-11-25.
- **Do ustalenia z marką:** treść e-maili, domena nadawcy e-maili (SPF/DKIM w Brevo). Regulamin i polityka — D25.

## Następne kroki (propozycja kolejności)

1. **Wdrożenie produkcyjne** — przygotowane ([deployment.md](deployment.md)). Zostało: wybór i założenie VPS, domena,
   pierwsze wdrożenie, kopie poza serwer (rclone/snapshoty), przećwiczenie odtwarzania. Adapter **S3** dla `FileStorage`
   odłożony (D22) — pliki na dysku serwera. Automatyczne wdrożenie po pushu (CD przez SSH) — gdy serwer będzie gotowy.
   **Wstrzymane (2026-11-24)** — wrócimy później. Rozważany serwer: **Mikrus 3.5** (4 GB RAM, LXC, 197 zł/rok). Wymaga
   trybu „za pośrednikiem”: brak własnego IPv4 i portów 80/443, więc HTTPS robi Cloudflare (AAAA + proxy, najlepiej
   Full strict z certyfikatem Origin) albo subdomena `*.mikrus.cloud`; Caddy po HTTP na IPv6, `trusted_proxies`
   pośrednika i adres klienta z jego nagłówka (inaczej rate limiting liczy wszystkich jako jednego klienta).
   Do sprawdzenia na serwerze: `docker compose` w LXC.
2. ~~Polityka prywatności~~ — zrobione z regulaminem i rejestrem zgód (D25, 2026-11-26). Zostało po stronie marki:
   uzupełnić pola w `[nawiasach]` we wzorach (panel → Dokumenty prawne), zatwierdzenie przez prawnika, okresy
   przechowywania danych (potem ewentualnie automatyczne usuwanie starych kont i zestawień).
3. **Import prawdziwych danych produktów** i packshotów (zamiast danych demonstracyjnych; potem `NEXT_PUBLIC_DEMO_NOTICE=false`).
4. ~~Wersja mobilna~~ — zrobione jako strona responsywna + PWA (D26, 2026-11-26). Dalej ewentualnie: powiadomienia
   push (nowy wniosek kupca, nowe zestawienie), edytor modelu na telefonie (zdjęcia z aparatu), aplikacja natywna.

## Backlog pomysłów (2026-10-09)

Pomysły do rozważenia — nie są zobowiązaniem. Przy realizacji przenieś pozycję do „Następnych kroków”
i zapisz decyzje w `decisions.md`.

### Dla kupców
- **Eksport zestawienia do Excela/CSV** obok PDF — do wklejenia w systemy zamówień sklepów.
- ~~Paczki rozmiarowe~~ — zrobione jako pełna rozmiarówka × liczba paczek (D21). Rozszerzenie: szablony proporcji (np. 1-2-2-1) w panelu.
- **Kolekcje i sezony** — strony kolekcji z sesją zdjęciową, oznaczenie nowości.
- **Termin dostępności przy modelu** (np. „wysyłka od 15.11”) — bez stanów magazynowych.
- **Udostępnianie zestawienia linkiem** (wspólnik, drugi sklep).
- **Linki per klient z tokenem** — kto co oglądał, w przyszłości ceny indywidualne (wcześniej świadomie odłożone).

### Dla marki (panel)
- **Import produktów z Excela** — potrzebny przy wprowadzaniu prawdziwej oferty.
- **Zestawienia jako mini-CRM** — status leada (nowy / w kontakcie / zamówił), notatki, licznik nowych leadów w nawigacji.
- **Operacje hurtowe** — zmiana cen o X% na sezon, duplikowanie modelu, ukrycie całej kategorii.
- **Optymalizacja zdjęć przy uploadzie** — kilka rozmiarów, WebP/AVIF.
- **Historia zmian** — kto i kiedy zmienił cenę lub status.

### Technika
- **Testy end-to-end frontendu w CI**, automatyczny audyt dostępności (axe) i wydajności (Lighthouse).
- **SEO dla nowych kupców** — sitemap, hreflang, podglądy linków (Open Graph) przy udostępnianiu w social media.
- **Monitoring i alerty** (logi, metryki, błędy) po wdrożeniu.

### Marka i prawo
- **Strona „O marce”** — szyte w Polsce, autorski design, warunki współpracy (ważne dla nowych kupców z Instagrama).

Rekomendowana kolejność (z rozmowy 2026-10-09): wdrożenie → import z Excela + eksport zestawienia do Excela →
zestawienia jako mini-CRM.

## Lista kontrolna przed produkcją

- [ ] `SHOWROOM_JWT_SECRET` (≥ 32 bajty, base64) i hasło pierwszego admina tylko w sekretach środowiska
- [x] `SHOWROOM_REFRESH_COOKIE_SECURE=true`, HTTPS na proxy — wymuszone w `deploy/compose.yml` / Caddy
- [x] Proxy ustawia `X-Forwarded-For` (rate limiting rozpoznaje klienta tylko od zaufanych proxy) — Caddy
- [ ] Rate limiting działa w pamięci jednej instancji — przy kilku instancjach wspólny backend (Redis/JCache)
- [x] `SHOWROOM_RATE_LIMIT_ENABLED` nie wyłączony — wymuszone w `deploy/compose.yml`
- [ ] Kopie zapasowe bazy i magazynu plików — `deploy/backup.sh` w cronie + kopia poza serwerem
- [ ] Odtworzenie z kopii przećwiczone
- [ ] SMTP dla e-maili kont kupców (`SMTP_*`, `SHOWROOM_MAIL_FROM`), SPF i DKIM na domenie nadawcy; test: rejestracja → e-mail dochodzi.
  Konto Brevo założone, wysyłka sprawdzona lokalnie (2026-11-25, nadawca tymczasowo z hotmail) — zostało: własna
  domena w Brevo z DKIM/DMARC i osobny klucz SMTP dla produkcji.
- [x] Regulamin i polityka prywatności podpięte w formularzu rejestracji kupca, rejestr zgód (D25)
- [ ] Dane marki w PDF: `SHOWROOM_BRAND_*` (nazwa, firma, adres, kontakt), opcjonalnie logo
- [ ] Prawdziwy opiekun handlowy w panelu (zamiast przykładowego)
- [ ] `NEXT_PUBLIC_DEMO_NOTICE=false` po imporcie prawdziwych danych
- [ ] Regulamin i polityka prywatności: pola `[…]` uzupełnione, treść zatwierdzona przez prawnika, uwaga „Wzór do weryfikacji” usunięta, nowa wersja opublikowana w panelu

## Czeka na markę / właściciela

- Dane administratora danych osobowych, okresy przechowywania i akceptacja treści regulaminu / polityki przez prawnika.
- Prawdziwe dane produktów (nazwy, składy, rozmiarówki, EAN, ceny) i packshoty.
- Adres grupy „Klienci hurtowi Fabian's Style” na Facebooku (do stopki).
- Logo w wersji wektorowej (SVG).
- Dane opiekunów handlowych i dane firmy do stopki PDF.

## Świadomie odłożone

- Dostęp kupców przez token w URL (link per klient).
- Wysyłka e-maili (decyzja D12 — obecnie żadnych e-maili).
- Licznik nowych zestawień w nawigacji panelu (pomysł z przeglądu).
