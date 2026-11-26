# B2B Showroom — backend

Interaktywny katalog B2B / lookbook marki odzieżowej: publiczny katalog z cenami hurtowymi i MSRP,
zestawienia ofertowe PDF (line sheets) do pobrania, analityka oraz panel administracyjny.
Aplikacja nie wysyła e-maili — kontakt z opiekunem handlowym odbywa się przez dane widoczne w katalogu i w PDF.

Java 21 · Spring Boot 3.5 · PostgreSQL 18 · Flyway · Spring Security (JWT) · Testcontainers · ArchUnit

## Uruchomienie lokalne

```bash
# z katalogu głównego repozytorium
docker compose up -d                       # PostgreSQL na porcie 5433
cp .env.example .env && $EDITOR .env       # uzupełnij SHOWROOM_JWT_SECRET i dane pierwszego admina
set -a; source .env; set +a

cd backend
./mvnw spring-boot:run
./mvnw test                                # 258 testów; wymagany Docker (Testcontainers)
```


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
| `SHOWROOM_BRAND_NAME`, `_LEGAL_NAME`, `_WEBSITE`, `_EMAIL`, `_PHONE` | nie | dane marki w nagłówku i stopce PDF (placeholdery do czasu identyfikacji); logo: `showroom.brand.logo` (PNG/JPEG z classpath) |
| `SHOWROOM_RATE_LIMIT_ENABLED` | nie | domyślnie `true`; wyłączać tylko w testach |
| `SHOWROOM_MAIL_ENABLED` | nie | e-maile transakcyjne kont kupców (D23); domyślnie `false` — treść (z linkiem) trafia tylko do logu |
| `SHOWROOM_MAIL_FROM` | przy e-mailach | nadawca, np. `Fabian's Style B2B <b2b@marka.pl>` (domena z SPF/DKIM) |
| `SHOWROOM_PUBLIC_BASE_URL` | przy e-mailach | publiczny adres showroomu — z niego linki w e-mailach (domyślnie `http://localhost:3000`) |
| `SPRING_MAIL_HOST`, `_PORT`, `_USERNAME`, `_PASSWORD` | przy e-mailach | serwer SMTP (port 587, STARTTLS wymagany); brak hosta przy włączonych e-mailach = błąd startu |

## Dane demonstracyjne

Do czasu importu prawdziwych danych marki katalog można wypełnić **danymi przykładowymi** zbudowanymi z prawdziwych
zdjęć z `brand-assets/` (katalog poza repozytorium):

```bash
scripts/dev-up.sh --seed     # baza + backend + dane demo (wymaga .env z SHOWROOM_ADMIN_EMAIL/PASSWORD)
scripts/dev-up.sh stop
```

Na Windowsie bez Dockera: `scripts\dev-up.ps1 -Seed` / `scripts\dev-up.ps1 stop` (natywny PostgreSQL 18, zob. `docs/development.md`).

`scripts/seed-demo.mjs` (Node 22, bez zależności) korzysta wyłącznie z REST API i jest idempotentny. Tworzy 3 kategorie,
15 kolorów nazwanych od printów, rozmiary 36–46, 9 modeli sukienek (7 opublikowanych, 1 ukryty, 1 szkic) z wariantami
i zdjęciami oraz przykładowego opiekuna handlowego. **Nazwy, opisy, ceny, SKU i EAN są ilustracyjne** — EAN-y leżą
w puli GS1 „restricted circulation” (prefiks 29), więc nie kolidują z prawdziwymi produktami; opisy mówią tylko o tym,
co widać na zdjęciach.

## Architektura heksagonalna

Moduły (bounded contexts): `catalog`, `linesheet`, `analytics`, `contact`, `identity`, `legal` + `shared` (kernel).
Każdy moduł ma tę samą strukturę:

```
pl.wizytowka.<moduł>
├── domain/                     czysta Java: agregaty, value objects, reguły (bez Springa/JPA/Jacksona)
├── application/
│   ├── port/in/                przypadki użycia (API modułu dla adapterów i innych modułów)
│   ├── port/out/               czego aplikacja potrzebuje od świata (repozytoria, storage, PDF…)
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
- **Zestawienia (line sheets)** zapisują migawkę pozycji (nazwy, EAN, ceny, zdjęcie) i danych kupca z jego konta — zmiana ceny, danych konta czy usunięcie produktu nie zmienia wysłanej oferty.
- **Konta kupców (D23)** są w module `identity` obok kont panelu (wspólne hashowanie haseł, blokady, rotacja sesji), ale mają osobne tabele, ciasteczko i rolę `BUYER`. Ceny i EAN zwraca wyłącznie `GET /buyer/catalog/pricing` — publiczne odpowiedzi katalogu ich nie zawierają, więc mogą być dalej współdzielone i cache'owane.
- **Pliki** zapisuje się przez port `FileStorage` (dziś dysk lokalny, docelowo S3). Klucze generuje serwer; zdjęcia są rozpoznawane po zawartości (magic bytes), nie po nagłówku klienta. Publiczny endpoint mediów serwuje wyłącznie `products/**`.

## Bezpieczeństwo

- Panel: JWT (HS256, 15 min) w nagłówku `Authorization: Bearer`; access token trzymany wyłącznie w pamięci frontendu.
- Refresh token: losowe 256 bitów w ciasteczku `showroom_refresh` (`HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/admin/auth`), w bazie tylko hash SHA-256, rotacja przy każdym użyciu. Ponowne użycie zużytego tokenu unieważnia całą sesję (wykrycie kradzieży).
- Kupcy (D23): ta sama mechanika z rolą `BUYER` — ciasteczko `showroom_buyer_refresh` (`Path=/api/v1/buyer/auth`, 30 dni), osobna tabela `buyer_refresh_token`. Token kupca nie wchodzi do panelu, token panelu — do API kupca. Logowanie na nieaktywne konto zwraca `403 identity.buyer.not-active` + `status` dopiero po poprawnym haśle.
- Rejestracja, ponowna wysyłka potwierdzenia i prośba o reset hasła zwracają zawsze `202` (nie zdradzają istnienia konta); tokeny z e-maili są jednorazowe, w bazie tylko hash (potwierdzenie 48 h, reset 1 h), nowy unieważnia poprzedni, kolejny e-mail tego rodzaju najwcześniej po 2 min. E-maile mają stałe szablony bez treści od użytkownika i wychodzą asynchronicznie po commicie.
- Endpointy `/api/v1/admin/auth/*` i `/api/v1/buyer/auth/*` wymagają nagłówka `X-Requested-With: XMLHttpRequest` (ochrona CSRF obok `SameSite=Strict`); filtr porównuje zdekodowaną ścieżkę, więc zakodowane znaki go nie omijają.
- Hasła: PBKDF2-HMAC-SHA256 (600 000 iteracji), polityka 12–128 znaków. Blokada konta po 5 nieudanych logowaniach na 15 minut. Odpowiedź przy błędnym logowaniu nie zdradza, czy konto istnieje (także czasem odpowiedzi).
- Role: `ADMIN` (wszystko, łącznie z kontami) i `EDITOR` (katalog, treści, zestawienia, analityka, wnioski kupców); `BUYER` — zaakceptowany kupiec (`/api/v1/buyer/**`). Każda niewymieniona ścieżka → `denyAll`.
- Nagłówki: HSTS, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy: no-referrer`, CSP `default-src 'none'`.
- RODO (D25): analityka nie zapisuje adresów IP ani User-Agenta. Regulamin i polityka prywatności są wersjonowane (moduł `legal`, wzory z `resources/legal/templates` publikowane jako wersja 1 przy pustej bazie — do weryfikacji prawnej); rejestr `buyer_consent` zapisuje, którą wersję kupiec zaakceptował (bez IP). Nowa istotna wersja regulaminu blokuje `/buyer/catalog/**` i `/buyer/line-sheets**` (`BuyerTermsGateFilter`, `403 identity.buyer.terms-acceptance-required`). Kupiec sam pobiera dane (JSON) i usuwa konto; usunięcie (także przez markę) usuwa z jego zestawień dane osoby kontaktowej i pliki PDF (zdarzenie `BuyerAccountDeleted`).
- **Rate limiting** (Bucket4j, per adres klienta; IPv6 per /64; kubełki w ograniczonym cache), filtr przed Spring Security, odpowiedź `429` + `Retry-After` + Problem Details `request.rate-limited`:

  | Endpoint | Limit |
  |---|---|
  | `POST /api/v1/buyer/line-sheets` | 5/min i 30/h |
  | `POST /api/v1/buyer/auth/register` · `/resend-verification` · `/password-reset/request` | po 3/min i 10/h |
  | `POST /api/v1/buyer/auth/verify-email` · `/password-reset/confirm` | 10/min |
  | `POST /api/v1/buyer/auth/login` | 10/min i 50/h |
  | `POST /api/v1/buyer/auth/refresh` | 30/min |
  | `POST /api/v1/buyer/me/password` · `/buyer/me/delete` | po 5/min |
  | `GET /api/v1/buyer/me/export` | 5/min i 30/h |
  | `POST /api/v1/analytics/events` | 60/min |
  | `POST /api/v1/admin/auth/login` | 10/min i 50/h |
  | `POST /api/v1/admin/auth/refresh` | 30/min |

  Adres klienta za reverse proxy: `server.forward-headers-strategy=native` — `X-Forwarded-For` jest honorowany wyłącznie od zaufanych (wewnętrznych) adresów proxy, więc proxy musi go ustawiać. Limity są w pamięci jednej instancji; przy skalowaniu poziomym potrzebny wspólny backend (Bucket4j JCache/Redis).

## Zestawienia PDF

Adapter `HtmlLineSheetPdfRenderer`: szablon Thymeleaf (`resources/pdf/templates/line-sheet.html`, sam rdzeń Thymeleaf — bez widoków MVC) → HTML/CSS → PDF przez OpenHTMLtoPDF (PDFBox).

- A4 poziomo, kompaktowa tabela: miniatura, model, kolor, rozmiar, EAN, SKU, ilość, cena hurtowa netto, sugerowana cena detaliczna brutto; nagłówek tabeli powtarzany na każdej stronie, podsumowanie, stopka „strona X z Y”.
- Etykiety w 4 językach (`resources/pdf/messages`), kwoty i daty według języka zestawienia, strefa czasowa marki.
- Font Noto Sans (SIL OFL 1.1, `resources/pdf/fonts/OFL.txt`) — pełne pokrycie polskich, czeskich i ukraińskich znaków.
- Zdjęcia: dekodowanie z subsamplingiem (JPEG/PNG/WebP, także JPEG CMYK), miniatury osadzane jako JPEG; brak lub nieobsługiwany format (AVIF) → placeholder, nigdy błąd całego dokumentu.
- Bezpieczeństwo: dane użytkownika tylko przez `th:text` (escapowanie), renderer nie pobiera żadnych zasobów zewnętrznych (wyłącznie `data:` URI i fonty z classpath).
- 200 pozycji ≈ 3 s i ≈ 300 KB.

## API (`/api/v1`)

Publiczne (bez cen i EAN — D23):

| Metoda | Ścieżka | Opis |
|---|---|---|
| GET | `/catalog/products` | lista: `category`, `color`*, `size`*, `q`, `featured`, `sort` (`RECOMMENDED`, `NEWEST`, `PRICE_ASC`, `PRICE_DESC`), `page`, `pageSize` |
| GET | `/catalog/products/{slug}` | karta produktu: kolory → warianty (rozmiar, SKU), zdjęcia |
| GET | `/catalog/categories` · `/catalog/facets` | drzewo kategorii · filtry z liczbą produktów |
| GET | `/media/products/**` | zdjęcia produktów (cache `immutable`) |
| GET | `/contact/representatives` | opiekunowie handlowi |
| GET | `/legal/{terms|privacy}` · `/legal/{terms|privacy}/versions` · `/legal/versions/{id}` | obowiązująca wersja dokumentu w języku żądania · historia wersji · konkretna opublikowana wersja (D25) |
| POST | `/analytics/events` | paczka zdarzeń z przeglądarki (max 50) |

Konta kupców — bez logowania (`X-Requested-With` wymagany, język e-maili z `?lang=` / `Accept-Language`):

| Metoda | Ścieżka | Opis |
|---|---|---|
| POST | `/buyer/auth/register` | `{email, password, companyName, countryCode, taxId, contactName, phone, termsAccepted, privacyAccepted, termsVersionId, privacyVersionId}` → `202` (zawsze); wersje inne niż obowiązujące → `409 identity.buyer.legal-version-outdated` |
| POST | `/buyer/auth/verify-email` · `/buyer/auth/resend-verification` | `{token}` → `204` · `{email}` → `202` |
| POST | `/buyer/auth/password-reset/request` · `/confirm` | `{email}` → `202` · `{token, password}` → `204` (kończy sesje) |
| POST | `/buyer/auth/login` · `/refresh` · `/logout` | sesja jak w panelu; `403 identity.buyer.not-active` + `status` dla nieaktywnego konta |

Zalogowany kupiec (rola `BUYER`):

| Metoda | Ścieżka | Opis |
|---|---|---|
| GET/PUT | `/buyer/me` | konto; zmiana osoby kontaktowej, telefonu, języka (`version`) |
| POST | `/buyer/me/password` | `{currentPassword, newPassword}` → nowa sesja, pozostałe zakończone |
| GET | `/buyer/me/legal` | `{pending:[{type, versionId, versionNumber, blocking, firstTime}], history:[…]}` — dokumenty do akceptacji i historia zgód |
| POST | `/buyer/me/legal/accept?lang=` | `{versionIds}` — akceptacja obowiązujących wersji (`409` dla nieaktualnej) |
| GET | `/buyer/me/export` | pobranie danych (JSON, `attachment`): konto, zgody, zestawienia |
| POST | `/buyer/me/delete` | `{currentPassword}` → `204`, konto usunięte, ciasteczko wyczyszczone, e-mail z potwierdzeniem |
| GET | `/buyer/catalog/pricing?product=…` | ceny i EAN aktywnych wariantów (maks. 100 produktów, `no-store`) |
| POST | `/buyer/line-sheets` | zestawienie PDF z danymi konta: `{items:[{variantId, quantity?}], message?, salesRepresentativeId?, visitorId?}` |
| GET | `/buyer/line-sheets` · `/buyer/line-sheets/{publicId}/pdf` | „Moje zestawienia” · PDF (tylko właściciel; żądanie z tokenem) |

Panel (`/admin`, JWT):

| Obszar | Endpointy |
|---|---|
| Auth | `POST /admin/auth/login`, `POST /admin/auth/refresh`, `POST /admin/auth/logout`, `GET /admin/auth/me` |
| Produkty | `GET/POST /admin/products`, `GET/PUT/DELETE /admin/products/{id}`, `POST …/{id}/status` |
| Warianty | `POST …/{id}/colors`, `DELETE …/{id}/colors/{colorId}`, `PUT …/{id}/colors/order`, `POST …/{id}/variants`, `PUT/DELETE …/{id}/variants/{variantId}` |
| Zdjęcia | `POST …/{id}/images` (multipart), `DELETE …/{id}/images/{imageId}`, `PUT …/{id}/images/order`, `PUT …/{id}/images/{imageId}/color` |
| Słowniki | `/admin/categories`, `/admin/colors`, `/admin/sizes` (GET, POST, PUT `/{id}`, DELETE `/{id}`) |
| Opiekunowie | `/admin/sales-representatives` (CRUD) |
| Zestawienia | `GET /admin/line-sheets`, `GET /admin/line-sheets/{id}`, `GET /admin/line-sheets/{id}/pdf` |
| Dokumenty prawne | `GET /admin/legal`, `GET/PUT/DELETE /admin/legal/versions/{id}` (szkic), `POST /admin/legal/{terms|privacy}/drafts`, `POST /admin/legal/versions/{id}/publish` (`{version, requireAcceptance}`, tylko ADMIN) |
| Kupcy | `GET /admin/buyers?status=&q=`, `GET /admin/buyers/pending-count`, `GET /admin/buyers/{id}` (z rejestrem zgód), `POST …/{id}/approve` · `/reject` · `/suspend` · `/reactivate` (`{version, note?}`), `DELETE /admin/buyers/{id}` |
| Analityka | `GET /admin/analytics/summary?from=&to=` |
| Konta (ADMIN) | `GET/POST /admin/users`, `PUT /admin/users/{id}`, `POST /admin/users/{id}/enable` · `/disable` · `/password` |

## Testy

| Rodzaj | Gdzie | Narzędzia |
|---|---|---|
| Domena i serwisy | `*/domain`, `*/application/service` | JUnit 5, AssertJ, fake'i w pamięci |
| Persystencja | `*/adapter/out/persistence` | `@PersistenceTest` (PostgreSQL 18 w Testcontainers, schemat z Flyway, `ddl-auto=validate`) |
| Web | `*/adapter/in/web` | `@WebMvcTest` + MockMvc |
| Bezpieczeństwo | `config/security` | pełny kontekst + JWT, rotacja tokenów, blokady |
| End-to-end | `ShowroomEndToEndTest` | admin → produkt → katalog bez cen → rejestracja kupca → akceptacja → ceny → zestawienie PDF → analityka |
| Architektura | `architecture` | ArchUnit |

## Następne kroki

Aktualny plan: [`docs/roadmap.md`](../docs/roadmap.md).
