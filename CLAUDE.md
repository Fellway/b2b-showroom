# B2B Showroom — kontekst projektu

Interaktywna wizytówka i katalog B2B marki odzieży damskiej **fabian's style** (produkcja w Polsce, autorski design).
Kontekst produktowy dla projektowania UI (użytkownicy, sytuacje, pozycjonowanie, zasady): [`PRODUCT.md`](PRODUCT.md). **To NIE jest sklep**: brak koszyka, płatności i stanów
magazynowych. Klient hurtowy przegląda katalog/lookbook, zaznacza warianty i generuje spersonalizowane
zestawienie PDF (line sheet) do pobrania, oraz kontaktuje się z opiekunem handlowym (widoczny e-mail/telefon).
**Aplikacja nie wysyła żadnych e-maili** (decyzja użytkownika, Etap 3).

Projekt prywatny, **całkowicie niezależny od Empiku** — patrz „Środowisko”.

Szczegóły techniczne, tabela API i instrukcja uruchomienia: [`backend/README.md`](backend/README.md).

## Plan etapów

| Etap | Zakres | Status |
|---|---|---|
| 1 | Model danych PostgreSQL + Flyway | ✅ |
| 2 | Backend heksagonalny, REST API (katalog, panel, zestawienia, analityka), JWT | ✅ 232 testy |
| 3 | Adapter PDF (Thymeleaf → HTML → OpenHTMLtoPDF), usunięcie wysyłki e-mail (migracja V2), rate limiting | ✅ 258 testów |
| 4 | Frontend Next.js: lookbook, pasek wyboru pozycji, modal zestawienia/PDF, panel admina | ⏭ następny |

Pracujemy etapami — nie piszemy całego systemu naraz. Przed startem etapu zadajemy pytania doprecyzowujące,
jeśli decyzja zmienia kształt rozwiązania.

## Założenia biznesowe (decyzje użytkownika)

- **Dostęp klientów:** katalog całkowicie publiczny, bez logowania i tokenów (może się zmienić później — np. linki z tokenem per klient).
- **Panel administracyjny:** zabezpieczony; role `ADMIN` (wszystko + konta) i `EDITOR` (katalog, treści, zestawienia, analityka).
- **Struktura produktu:** Produkt → Kolor → Rozmiar → wariant z SKU i EAN (GTIN-8/12/13/14, walidowana cyfra kontrolna). SKU i EAN globalnie unikalne.
- **Ceny tylko na poziomie produktu:** hurtowa (netto) + sugerowana detaliczna MSRP/RRP (brutto). Waluta PLN na start (model wspiera ISO 4217).
- **Cykl życia produktu:** `DRAFT` → `PUBLISHED` ⇄ `HIDDEN`, `ARCHIVED` (przywracany do `DRAFT`). Publikacja wymaga ≥1 aktywnego wariantu i opublikowany produkt zawsze go zachowuje. Usunięcie jest trwałe — historyczne zestawienia zachowują migawki.
- **Brak śledzenia stanów magazynowych.**
- **Języki:** `pl` (bazowy i fallback), `en`, `cs`, `uk` — interfejs i treści produktów. Język żądania: `?lang=` → `Accept-Language` → `pl`.
- **Zestawienie PDF:** 1–200 różnych wariantów, opcjonalna ilość orientacyjna, kody EAN, zdjęcia, ceny hurtowe i sugerowane. Pozycje to **migawki** z chwili generowania. Klient tylko pobiera PDF (bez wysyłki e-mail). Podanie jakichkolwiek danych kontaktowych wymaga zgody RODO.
  Układ PDF: kompaktowa tabela (wiersz na wariant z miniaturą); dane marki to placeholdery w `showroom.brand.*` do dopracowania w Etapie 4. Domyślnie przypisywany pierwszy aktywny opiekun handlowy.
- **Analityka:** odwiedziny (unikalni odwiedzający), najczęściej oglądane produkty, najczęściej dodawane do wyboru, najczęściej występujące w zestawieniach, liczba zestawień. Bez IP i User-Agenta. Zdarzenia z przeglądarki: paczki ≤50, czas nadaje serwer; typy `LINE_SHEET_*` tylko z serwera. Dashboard: domyślnie 30 dni, max 366.

## Stos technologiczny

- **Backend** (`backend/`): Java 21, Spring Boot 3.5.x (zostajemy na 3.x), Spring Data JPA/Hibernate 6, Spring Security + OAuth2 Resource Server (JWT), Bean Validation, Actuator.
- **Baza:** PostgreSQL 18 (`postgres:18.4-alpine`), migracje Flyway 11.20.x, Hibernate wyłącznie waliduje schemat (`ddl-auto=validate`).
- **Pliki:** port `FileStorage` — dziś dysk lokalny, docelowo S3.
- **Frontend (Etap 4):** Next.js (App Router, TypeScript), Tailwind CSS, shadcn/ui, next-intl. **UI/UX projektujemy wtyczką `/impeccable`** — `PRODUCT.md` gotowy (init ✅); kierunek wizualny ustala `new-work` i zapisuje w `DESIGN.md`.
- **Wdrożenie:** frontend i API na **tej samej domenie** za reverse proxy (`/api` → Spring Boot). Brak CORS.

## Architektura backendu — heksagonalna z czystą domeną

```
pl.wizytowka.<moduł>            moduły: catalog, linesheet, analytics, contact, identity (+ shared, config)
├── domain/                     czysta Java — ZERO Springa, JPA, Jacksona, jakarta.*
├── application/port/in/        przypadki użycia = publiczne API modułu
├── application/port/out/       repozytoria, storage, PDF, tokeny…
├── application/service/        implementacje (wolno: @Service, @Transactional, java.time.Clock)
├── application/view/           widoki odczytu (CQRS-light)
└── adapter/in/web | adapter/out/<technologia>
```

Reguły (egzekwowane przez `HexagonalArchitectureTest`, ArchUnit — nie łamać, nie wyłączać):
- domena nie zależy od frameworków ani warstw zewnętrznych; aplikacja nie zna adapterów/JPA/web;
- adaptery wejściowe używają wyłącznie portów wejściowych;
- moduł korzysta z innego modułu tylko przez jego `application.port.in`, `application.view`, `domain`;
- `shared` nie zależy od modułów.

Konwencje:
- **Id nadaje domena:** typowane rekordy (`ProductId`…) z UUIDv7 (`Uuids.v7()`). Publiczne linki do PDF: osobny losowy UUIDv4 (`publicId`).
- **Agregaty:** prywatny konstruktor + `create(...)` / `restore(...)`, pole `long version`. Wszystkie zmiany dzieci przez korzeń agregatu.
- **Persystencja:** `XxxJpaEntity` + `XxxJpaRepository` + `XxxPersistenceAdapter`; `@Version Long version` (null = nowy); adapter porównuje wersję domeny i synchronizuje kolekcje dzieci po id (nie podmienia zarządzanych kolekcji). Dzieci produktu usuwa agregat — FK do `product_color` są `DEFERRABLE INITIALLY DEFERRED` bez kaskad.
- **Schemat:** zmiany wyłącznie nowymi migracjami Flyway (`V2__…`). V1 traktujemy jako wdrożone od teraz.
- **Błędy:** podklasy `sealed DomainException` (`InvalidValue` 400, `BusinessRuleViolation` 422, `ResourceNotFound` 404, `ResourceConflict` 409, `ConcurrentModification` 409) ze stabilnym `code` (`<moduł>.<rzecz>.<problem>`); API zwraca RFC 9457 Problem Details + `code`, frontend tłumaczy kod na komunikat.
- **Optymistyczne blokowanie:** edycje w panelu przesyłają `version`.
- **Web:** publiczne `/api/v1/...`, panel `/api/v1/admin/...`; tworzenie → 201 + Location + `{id}`; stronicowanie `{items, page, size, totalItems, totalPages}`; język przez `@RequestedLanguage CatalogLanguage`. W publicznym katalogu rozmiar strony to `pageSize` (`size` = filtr rozmiarów).
- **Kod:** identyfikatory i komunikaty po angielsku, Javadoc/komentarze po polsku tam, gdzie wnoszą wartość; bez Lomboka; rekordy, sealed, pattern matching tam, gdzie uzasadnione; SOLID; OWASP.

## Bezpieczeństwo (decyzje)

- **Panel: bezstanowy JWT** (HS256, 15 min, tylko w pamięci frontendu) + **rotowany refresh token** w ciasteczku `showroom_refresh` (HttpOnly, Secure, SameSite=Strict, Path=`/api/v1/admin/auth`, 14 dni), w bazie tylko hash SHA-256; ponowne użycie zużytego tokenu unieważnia całą rodzinę.
- `POST /api/v1/admin/auth/*` wymaga nagłówka `X-Requested-With: XMLHttpRequest` (CSRF). Frontend nie może odświeżać tokenu równolegle (dwa refresh-e = wykrycie kradzieży = wylogowanie).
- Hasła: PBKDF2-HMAC-SHA256 600k iteracji, 12–128 znaków; blokada po 5 nieudanych logowaniach na 15 min; logowanie nie zdradza istnienia konta.
- Ścieżki spoza listy → `denyAll`. Publiczny endpoint mediów serwuje wyłącznie `products/**` (PDF-y z danymi klientów nigdy).
- Upload zdjęć: format rozpoznawany po magic bytes (JPEG/PNG/WebP/AVIF), max 10 MB, klucz generuje serwer.
- **Rate limiting** (Bucket4j, per IP, IPv6 per /64, filtr przed Spring Security): zestawienia 5/min i 30/h (generowanie PDF jest kosztowne), analityka 60/min, logowanie 10/min i 50/h, refresh 30/min. Za proxy: `server.forward-headers-strategy=native`. Testy pełnego kontekstu z wieloma logowaniami ustawiają `showroom.rate-limit.enabled=false`.
- **PDF:** dane użytkownika tylko przez `th:text`; renderer nie pobiera zasobów zewnętrznych (tylko `data:` URI i fonty z classpath).

## Środowisko i polecenia

- **Nie używamy infrastruktury Empiku** (Artifactory, certyfikaty, rejestry Dockera, `~/.m2/settings.xml`). Maven idzie przez `backend/.mvn/settings.xml` (tylko Maven Central, repozytorium `~/.m2/b2b-showroom-repository`), podpięte w `.mvn/maven.config`. Obrazy Dockera z publicznego Docker Huba.
- Polecenia Maven zawsze przez wrapper z katalogu `backend/`: `./mvnw test`, `./mvnw spring-boot:run` (Maven 3.9.16).
- Baza lokalnie: `docker compose up -d` (port 5433). Konfiguracja: `.env.example` → `.env` (`SHOWROOM_JWT_SECRET` wymagany: `openssl rand -base64 48`).
- Testy wymagają Dockera (Testcontainers). Slice JPA: `@PersistenceTest`; pełny kontekst: `@SpringBootTest` + `TestcontainersConfiguration` + właściwości `showroom.security.jwt.secret` i `showroom.storage.local-root`.
- Nie tworzyć `src/test/resources/application.yml` (przesłania główny plik konfiguracyjny).
- Git: lokalna tożsamość repozytorium to `Mateusz Skrzypczyk <33421875+Fellway@users.noreply.github.com>` (GitHub: [Fellway](https://github.com/Fellway)). **Nigdy nie używać maila z Empiku.**

## Otwarte tematy

- Adapter S3 dla `FileStorage`.
- Identyfikacja marki (nazwa, logo, kolory, typografia) — dziś placeholdery w `showroom.brand.*`; ustalimy w Etapie 4 (`/impeccable init`) i przeniesiemy też do PDF.
- Ewentualny dostęp klientów przez token w URL (świadomie odłożony).
