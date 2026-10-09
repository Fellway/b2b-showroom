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

Aplikacja działa lokalnie end-to-end na danych demonstracyjnych. Backend: 258 testów zielonych; frontend: typy,
lint i build bez błędów.

## Następne kroki (propozycja kolejności)

1. **Wdrożenie produkcyjne** — obrazy Docker (backend, frontend), reverse proxy z HTTPS (jedna domena: `/` → Next.js,
   `/api` → Spring Boot), PostgreSQL z kopiami zapasowymi, sekrety poza repozytorium, CI na GitHubie (testy backendu
   z Testcontainers, lint/typy/build frontendu), adapter **S3** dla `FileStorage` (zdjęcia i PDF-y).
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
- [ ] `SHOWROOM_REFRESH_COOKIE_SECURE=true` (domyślnie), HTTPS na proxy
- [ ] Proxy ustawia `X-Forwarded-For` (rate limiting rozpoznaje klienta tylko od zaufanych proxy)
- [ ] Rate limiting działa w pamięci jednej instancji — przy kilku instancjach wspólny backend (Redis/JCache)
- [ ] `SHOWROOM_RATE_LIMIT_ENABLED` nie wyłączony (wyłączamy go tylko w dev)
- [ ] Kopie zapasowe bazy i magazynu plików
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
