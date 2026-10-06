# B2B Showroom — backend

Interaktywny katalog B2B / lookbook marki odzieżowej: publiczny katalog z cenami hurtowymi i MSRP,
zestawienia ofertowe PDF (line sheets) wysyłane e-mailem, analityka oraz panel administracyjny.

Java 21 · Spring Boot 3.5 · PostgreSQL 18 · Flyway · Spring Security (JWT) · Testcontainers · ArchUnit

## Uruchomienie lokalne

```bash
# z katalogu głównego repozytorium
docker compose up -d                       # PostgreSQL na porcie 5433
cp .env.example .env && $EDITOR .env       # uzupełnij SHOWROOM_JWT_SECRET i dane pierwszego admina
set -a; source .env; set +a

cd backend
./mvnw spring-boot:run
./mvnw test                                # 232 testy; wymagany Docker (Testcontainers)
```

> **Etap 2:** adaptery `LineSheetDocumentRenderer` (PDF) i `LineSheetMailer` (SMTP) powstają w Etapie 3.
> Do tego czasu aplikacja nie wystartuje poza testami (testy pełnego kontekstu używają fake'ów
> z `support/LineSheetOutputFakesConfiguration`).

Projekt jest niezależny od infrastruktury firmowej: `.mvn/maven.config` podpina `.mvn/settings.xml`
(wyłącznie Maven Central, osobne repozytorium lokalne `~/.m2/b2b-showroom-repository`), a Maven Wrapper
pobiera Maven 3.9.16. Polecenia Maven uruchamiaj z katalogu `backend/`.

### Konfiguracja (zmienne środowiskowe)

| Zmienna | Wymagana | Opis |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | nie (dev) | domyślnie baza z `docker-compose.yml` |
| `SHOWROOM_JWT_SECRET` | **tak** | base64, min. 32 bajty: `openssl rand -base64 48`; brak = błąd startu |
| `SHOWROOM_JWT_ISSUER` | nie | wystawca tokenów |
| `SHOWROOM_REFRESH_COOKIE_SECURE` | nie | `false` wyłącznie lokalnie po HTTP |
| `SHOWROOM_ADMIN_EMAIL`, `SHOWROOM_ADMIN_PASSWORD` | nie | tworzą pierwsze konto ADMIN, gdy w bazie nie ma żadnego |
| `STORAGE_LOCAL_ROOT` | nie | katalog plików (zdjęcia, PDF); domyślnie `./data/storage` |

## Architektura heksagonalna

Moduły (bounded contexts): `catalog`, `linesheet`, `analytics`, `contact`, `identity` + `shared` (kernel).
Każdy moduł ma tę samą strukturę:

```
pl.wizytowka.<moduł>
├── domain/                     czysta Java: agregaty, value objects, reguły (bez Springa/JPA/Jacksona)
├── application/
│   ├── port/in/                przypadki użycia (API modułu dla adapterów i innych modułów)
│   ├── port/out/               czego aplikacja potrzebuje od świata (repozytoria, storage, PDF, mail…)
│   ├── service/                implementacje przypadków użycia (@Service, @Transactional)
│   └── view/                   widoki odczytu (CQRS-light)
└── adapter/
    ├── in/web/                 kontrolery REST + DTO
    └── out/persistence|…       JPA, Nimbus JWT, dysk, ImageIO…
```

Reguły pilnowane przez `HexagonalArchitectureTest` (ArchUnit):

- domena nie zależy od frameworków ani od warstw zewnętrznych;
- aplikacja nie zna adapterów, JPA ani warstwy web;
- adaptery wejściowe korzystają wyłącznie z portów wejściowych;
- moduły rozmawiają tylko przez `application.port.in`, `application.view` i `domain` innego modułu;
- `shared` nie zależy od żadnego modułu.

Kluczowe decyzje:

- **Identyfikatory nadaje domena** (UUIDv7 — rosnące w czasie). Linki do PDF używają osobnego, losowego `publicId` (UUIDv4), które nie zdradza czasu utworzenia.
- **Optymistyczne blokowanie:** każda edycja w panelu przesyła `version`; konflikt → `409 data.concurrent-modification`.
- **Błędy:** RFC 9457 Problem Details z własnością `code` (np. `catalog.variant.ean-taken`), którą frontend tłumaczy na komunikat w języku użytkownika. Hierarchia `DomainException` jest `sealed` — mapowanie na statusy HTTP to wyczerpujący `switch`.
- **i18n:** treści w tabelach `*_translation`; `pl` jest językiem bazowym i fallbackiem. Język żądania: `?lang=` → `Accept-Language` → `pl`.
- **Zestawienia (line sheets)** zapisują migawkę pozycji (nazwy, EAN, ceny, zdjęcie) — zmiana ceny czy usunięcie produktu nie zmienia wysłanej oferty.
- **Pliki** zapisuje się przez port `FileStorage` (dziś dysk lokalny, docelowo S3). Klucze generuje serwer; zdjęcia są rozpoznawane po zawartości (magic bytes), nie po nagłówku klienta. Publiczny endpoint mediów serwuje wyłącznie `products/**`.

## Bezpieczeństwo

- Panel: JWT (HS256, 15 min) w nagłówku `Authorization: Bearer`; access token trzymany wyłącznie w pamięci frontendu.
- Refresh token: losowe 256 bitów w ciasteczku `showroom_refresh` (`HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/admin/auth`), w bazie tylko hash SHA-256, rotacja przy każdym użyciu. Ponowne użycie zużytego tokenu unieważnia całą sesję (wykrycie kradzieży).
- Endpointy `/api/v1/admin/auth/*` wymagają nagłówka `X-Requested-With: XMLHttpRequest` (ochrona CSRF obok `SameSite=Strict`).
- Hasła: PBKDF2-HMAC-SHA256 (600 000 iteracji), polityka 12–128 znaków. Blokada konta po 5 nieudanych logowaniach na 15 minut. Odpowiedź przy błędnym logowaniu nie zdradza, czy konto istnieje (także czasem odpowiedzi).
- Role: `ADMIN` (wszystko, łącznie z kontami) i `EDITOR` (katalog, treści, zestawienia, analityka). Każda niewymieniona ścieżka → `denyAll`.
- Nagłówki: HSTS, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy: no-referrer`, CSP `default-src 'none'`.
- RODO: analityka nie zapisuje adresów IP ani User-Agenta; dane kontaktowe w zestawieniu wymagają zgody.

## API (`/api/v1`)

Publiczne:

| Metoda | Ścieżka | Opis |
|---|---|---|
| GET | `/catalog/products` | lista: `category`, `color`*, `size`*, `q`, `featured`, `sort` (`RECOMMENDED`, `NEWEST`, `PRICE_ASC`, `PRICE_DESC`), `page`, `pageSize` |
| GET | `/catalog/products/{slug}` | karta produktu: kolory → warianty (rozmiar, SKU, EAN), zdjęcia, ceny |
| GET | `/catalog/categories` · `/catalog/facets` | drzewo kategorii · filtry z liczbą produktów |
| GET | `/media/products/**` | zdjęcia produktów (cache `immutable`) |
| GET | `/contact/representatives` | opiekunowie handlowi |
| POST | `/line-sheets` | utworzenie zestawienia PDF (opcjonalnie z wysyłką e-mail) |
| GET | `/line-sheets/{publicId}/pdf` | pobranie PDF |
| POST | `/analytics/events` | paczka zdarzeń z przeglądarki (max 50) |

Panel (`/admin`, JWT):

| Obszar | Endpointy |
|---|---|
| Auth | `POST /admin/auth/login`, `POST /admin/auth/refresh`, `POST /admin/auth/logout`, `GET /admin/auth/me` |
| Produkty | `GET/POST /admin/products`, `GET/PUT/DELETE /admin/products/{id}`, `POST …/{id}/status` |
| Warianty | `POST …/{id}/colors`, `DELETE …/{id}/colors/{colorId}`, `PUT …/{id}/colors/order`, `POST …/{id}/variants`, `PUT/DELETE …/{id}/variants/{variantId}` |
| Zdjęcia | `POST …/{id}/images` (multipart), `DELETE …/{id}/images/{imageId}`, `PUT …/{id}/images/order`, `PUT …/{id}/images/{imageId}/color` |
| Słowniki | `/admin/categories`, `/admin/colors`, `/admin/sizes` (GET, POST, PUT `/{id}`, DELETE `/{id}`) |
| Opiekunowie | `/admin/sales-representatives` (CRUD) |
| Zestawienia | `GET /admin/line-sheets`, `GET /admin/line-sheets/{id}`, `POST /admin/line-sheets/{id}/resend-email` |
| Analityka | `GET /admin/analytics/summary?from=&to=` |
| Konta (ADMIN) | `GET/POST /admin/users`, `PUT /admin/users/{id}`, `POST /admin/users/{id}/enable` · `/disable` · `/password` |

## Testy

| Rodzaj | Gdzie | Narzędzia |
|---|---|---|
| Domena i serwisy | `*/domain`, `*/application/service` | JUnit 5, AssertJ, fake'i w pamięci |
| Persystencja | `*/adapter/out/persistence` | `@PersistenceTest` (PostgreSQL 18 w Testcontainers, schemat z Flyway, `ddl-auto=validate`) |
| Web | `*/adapter/in/web` | `@WebMvcTest` + MockMvc |
| Bezpieczeństwo | `config/security` | pełny kontekst + JWT, rotacja tokenów, blokady |
| End-to-end | `ShowroomEndToEndTest` | admin → produkt → katalog → zestawienie PDF → e-mail → analityka |
| Architektura | `architecture` | ArchUnit |

## Następne kroki

- **Etap 3:** adapter PDF (HTML → PDF), JavaMailSender z asynchroniczną wysyłką i ponowieniami, rate limiting publicznych POST-ów (zestawienia z e-mailem, analityka).
- **Etap 4:** frontend Next.js (lookbook, wybór pozycji, generator PDF, panel) — projekt UI/UX z wtyczką Impeccable.
- Adapter S3 dla `FileStorage`.
