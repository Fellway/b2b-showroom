# Status i plan

## Gdzie jesteśmy (2026-10-09)

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

Aplikacja działa lokalnie end-to-end na danych demonstracyjnych. Backend: 258 testów zielonych; frontend: typy,
lint i build bez błędów. Repozytorium: [github.com/Fellway/b2b-showroom](https://github.com/Fellway/b2b-showroom).

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
  (rejestracja → potwierdzenie → akceptacja → ceny → PDF). Etap 6 jest na gałęzi `feature/buyer-accounts` —
  6a i 6b trafiają na `main` razem.
- **Do ustalenia z marką:** treść e-maili, regulamin i zgody przy rejestracji (polityka prywatności staje się
  pilniejsza — zbieramy dane firm i osób), dostawca SMTP i adres nadawcy (SPF/DKIM na domenie).

## Następne kroki (propozycja kolejności)

1. **Wdrożenie produkcyjne** — przygotowane ([deployment.md](deployment.md)). Zostało: wybór i założenie VPS, domena,
   pierwsze wdrożenie, kopie poza serwer (rclone/snapshoty), przećwiczenie odtwarzania. Adapter **S3** dla `FileStorage`
   odłożony (D22) — pliki na dysku serwera. Automatyczne wdrożenie po pushu (CD przez SSH) — gdy serwer będzie gotowy.
   **Wstrzymane (2026-11-24)** — wrócimy później. Rozważany serwer: **Mikrus 3.5** (4 GB RAM, LXC, 197 zł/rok). Wymaga
   trybu „za pośrednikiem”: brak własnego IPv4 i portów 80/443, więc HTTPS robi Cloudflare (AAAA + proxy, najlepiej
   Full strict z certyfikatem Origin) albo subdomena `*.mikrus.cloud`; Caddy po HTTP na IPv6, `trusted_proxies`
   pośrednika i adres klienta z jego nagłówka (inaczej rate limiting liczy wszystkich jako jednego klienta).
   Do sprawdzenia na serwerze: `docker compose` w LXC.
2. **Polityka prywatności** — strona z danymi administratora danych; po publikacji ustawić
   `NEXT_PUBLIC_PRIVACY_POLICY_URL` (link w banerze i stopce). Szablon możemy przygotować; treść prawna i dane — od marki.
3. **Import prawdziwych danych produktów** i packshotów (zamiast danych demonstracyjnych; potem `NEXT_PUBLIC_DEMO_NOTICE=false`).
4. **Wersja mobilna** katalogu i panelu, potem aplikacja mobilna (API jest gotowe).

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
- **Polityka prywatności** — szablon do uzupełnienia przez markę / prawnika.
- **Strona „O marce”** — szyte w Polsce, autorski design, warunki współpracy (ważne dla nowych kupców z Instagrama).
- **Wersja mobilna** katalogu i panelu, potem aplikacja mobilna.

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
- [ ] Regulamin i polityka prywatności podpięte w formularzu rejestracji kupca (zgody są wymagane)
- [ ] Dane marki w PDF: `SHOWROOM_BRAND_*` (nazwa, firma, adres, kontakt), opcjonalnie logo
- [ ] Prawdziwy opiekun handlowy w panelu (zamiast przykładowego)
- [ ] `NEXT_PUBLIC_DEMO_NOTICE=false` po imporcie prawdziwych danych
- [ ] Polityka prywatności opublikowana i podpięta

## Czeka na markę / właściciela

- Polityka prywatności i dane administratora danych osobowych.
- Prawdziwe dane produktów (nazwy, składy, rozmiarówki, EAN, ceny) i packshoty.
- Adres grupy „Klienci hurtowi Fabian's Style” na Facebooku (do stopki).
- Logo w wersji wektorowej (SVG).
- Dane opiekunów handlowych i dane firmy do stopki PDF.

## Świadomie odłożone

- Dostęp kupców przez token w URL (link per klient).
- Wysyłka e-maili (decyzja D12 — obecnie żadnych e-maili).
- Licznik nowych zestawień w nawigacji panelu (pomysł z przeglądu).
