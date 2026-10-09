# Rejestr decyzji

Każda decyzja: **co** postanowiono, **dlaczego** i **co z tego wynika**. Decyzje oznaczone *(użytkownik)* podjął
właściciel projektu — nie zmieniamy ich bez pytania. Pozostałe to decyzje techniczne podjęte w trakcie pracy;
można je rewidować, jeśli zmienią się okoliczności. Nowe decyzje dopisuj na końcu.

---

## D1. Katalog publiczny, bez kont i tokenów dla kupców *(użytkownik, 2026-10-05)*
- **Dlaczego:** najprostsza ścieżka dla kupca; link z tokenem per klient świadomie odłożony.
- **Skutki:** ceny hurtowe są publiczne; ochrona publicznych POST-ów przez rate limiting (D13). Tabela kont dotyczy tylko adminów.

## D2. Ceny tylko na poziomie modelu *(użytkownik, 2026-10-05)*
- Cena hurtowa netto + sugerowana detaliczna brutto (MSRP), waluta PLN (model wspiera ISO 4217). Brak cen per wariant.

## D3. Tłumaczenia w tabelach `*_translation`, `pl` jako język bazowy *(użytkownik, 2026-10-05)*
- **Dlaczego:** spójność wymuszana kluczami, łatwe dodanie języka, wyszukiwanie per język.
- **Skutki:** każda treść musi mieć `pl`; brakujący język = fallback do `pl` (backend i PDF).

## D4. Niezależność od infrastruktury Empiku *(użytkownik, 2026-10-05 i 2026-10-06)*
- Maven wyłącznie z Maven Central przez `backend/.mvn/settings.xml` (podpięty w `.mvn/maven.config`), osobne repozytorium lokalne `~/.m2/b2b-showroom-repository`, Maven Wrapper 3.9.16.
- Git: lokalna tożsamość `Mateusz Skrzypczyk <33421875+Fellway@users.noreply.github.com>` (GitHub: Fellway). Nigdy maila z Empiku.
- npm: publiczny rejestr (`frontend/.npmrc`), obrazy Dockera z Docker Huba.

## D5. Flyway 11.20.x zamiast domyślnego 11.7 *(użytkownik, 2026-10-05)*
- **Dlaczego:** oficjalne wsparcie PostgreSQL 18 (wcześniej ostrzeżenie przy starcie).

## D6. Architektura heksagonalna z czystą domeną *(użytkownik, 2026-10-05)*
- Domena bez Springa/JPA/Jacksona; encje JPA i mappery w adapterach; granice pilnuje ArchUnit (`HexagonalArchitectureTest`).
- **Skutek techniczny:** identyfikatory nadaje domena (UUIDv7), żeby agregat nie czekał na id z bazy. Migrację V1 przepisano przed wdrożeniem; **od 2026-10-06 V1 traktujemy jako wdrożoną** — zmiany schematu tylko nowymi migracjami.

## D7. Panel: bezstanowy JWT + rotowany refresh token *(użytkownik, 2026-10-05)*
- Access token HS256 15 min tylko w pamięci frontendu; refresh token 14 dni w ciasteczku HttpOnly/Secure/SameSite=Strict (`Path=/api/v1/admin/auth`), w bazie tylko hash SHA-256; ponowne użycie zużytego tokenu unieważnia całą rodzinę (wykrycie kradzieży).
- CSRF dla endpointów auth: nagłówek `X-Requested-With: XMLHttpRequest` (wybrany zamiast sprawdzania Origin, bo reverse proxy przepisuje nagłówki).
- **Skutek dla frontendu:** odświeżanie musi być single-flight (dwa równoległe refresh-e = wylogowanie).

## D8. Frontend i API na jednej domenie *(użytkownik, 2026-10-05)*
- Reverse proxy: `/` → Next.js, `/api` → Spring Boot. Brak CORS, ciasteczka first-party. Lokalnie odwzorowuje to rewrite w `next.config.ts`.

## D9. Hasła: PBKDF2-HMAC-SHA256 (600 000 iteracji) zamiast bcrypt (2026-10-05)
- **Dlaczego:** bcrypt obcina hasło po 72 bajtach, a polityka dopuszcza 12–128 znaków; Argon2 wymagałby BouncyCastle.

## D10. Błędy jako `sealed DomainException` ze stabilnym kodem (2026-10-05)
- API zwraca RFC 9457 Problem Details + `code` (np. `catalog.variant.ean-taken`); frontend tłumaczy po kodzie, nigdy po treści.

## D11. Wariant dostępny w zestawieniu tylko w widocznej kategorii (2026-10-05)
- **Dlaczego:** produkt w ukrytej kategorii jest dla kupca niewidoczny, więc nie może trafić do zestawienia.

## D12. Aplikacja nie wysyła żadnych e-maili *(użytkownik, 2026-10-06)*
- Kupiec tylko pobiera PDF; kontakt z opiekunem przez widoczny e-mail/telefon. Usunięto port mailera, stan wysyłki i endpoint resend (migracja V2).
- **Dlaczego:** zero ryzyka wysyłki spamu z domeny marki przez publiczny endpoint.

## D13. Rate limiting publicznych i wrażliwych POST-ów (2026-10-06)
- Bucket4j w pamięci, klucz = adres klienta (IPv6 per /64), filtr przed Spring Security:
  zestawienia 5/min i 30/h, analityka 60/min, logowanie 10/min i 50/h, refresh 30/min.
- Za proxy: `server.forward-headers-strategy=native` (X-Forwarded-For tylko od zaufanych proxy).
- **Ograniczenie:** jedna instancja; przy skalowaniu potrzebny wspólny backend (Bucket4j JCache/Redis).

## D14. PDF: szablon Thymeleaf → OpenHTMLtoPDF, kompaktowa tabela *(układ: użytkownik, 2026-10-06)*
- A4 poziomo, wiersz na wariant z miniaturą; etykiety w 4 językach; font Noto Sans (OFL).
- Thymeleaf to sam rdzeń (bez widoków MVC) — służy tylko do składania HTML dokumentu z automatycznym escapowaniem.
- Renderer nie pobiera żadnych zasobów zewnętrznych (tylko `data:` URI i fonty z classpath).

## D15. Marka i treści *(użytkownik, 2026-10-06)*
- Marka **Fabian's Style** (odzież damska; wyróżniki: szyte w Polsce, autorski design, atrakcyjna marża). Rynki: PL, potem CZ/UA, EN dla pozostałych; ton profesjonalny i bezpośredni.
- Zdjęcia wygenerowane przez AI są używane na równi ze zdjęciami z sesji, także przy produktach.
- Dane produktów na razie demonstracyjne i oznaczone jako przykładowe (`NEXT_PUBLIC_DEMO_NOTICE`). Nie wymyślamy opinii, nagród, certyfikatów ani składu materiałów.

## D16. Kierunek wizualny: standard kategorii na poziomie COS / Arket *(użytkownik, 2026-10-06)*
- Wybrany zamiast wylosowanych kierunków (m.in. „Stół krojczy”). Biel, czerń logo jako jedyny kolor interfejsu, zdjęcia 2:3, jedna groteska z cyrylicą (Golos Text).
- Ryzyka wskazane przez użytkownika: nie może wyglądać jak sklep detaliczny, arkusz kalkulacyjny ani przekrzykiwać printów.
- System opisuje `DESIGN.md`; przebieg procesu — [design-process.md](design-process.md).

## D17. Panel: „sekcje + pełnoekranowy edytor” *(użytkownik, 2026-10-09)*
- Jedna osoba, często; najczęstsze zadania: nowe modele ze zdjęciami, ceny i ukrywanie, przegląd zestawień, statystyki.
- Tłumaczenia EN/CS/UK przychodzą z zewnątrz i są wklejane → panel pokazuje braki (`missingLanguages` w API).
- Panel po polsku, poza routingiem językowym.

## D18. Desktop teraz, mobile i aplikacja później *(użytkownik, 2026-10-09)*
- Logika wyłącznie w REST API (gotowe pod aplikację mobilną), układy projektowane tak, by dało się je przeskładać.

## D19. Akcje nieodwracalne to tekst, nie kolor (2026-10-09)
- Czerwień (`danger`) wyłącznie dla błędów; „Usuń” jako podkreślony tekst + potwierdzenie z czarnym przyciskiem i konkretnym czasownikiem.
- Krawędzie kontrolek w spoczynku: token `edge` (#8a8782, kontrast 3,6:1 — WCAG 1.4.11).

## D20. Baner zgody na analitykę *(użytkownik, 2026-10-09)*
- Bez zgody zdarzenia w pełni anonimowe — nic nie zapisujemy w przeglądarce. Po zgodzie: losowy identyfikator odwiedzającego (localStorage, odnawiany po 13 mies.) i sesji.
- Równorzędne „Akceptuję / Odrzucam”, nieblokujący, decyzja ważna 12 miesięcy, zmiana z linku w stopce; wycofanie lub wygaśnięcie usuwa identyfikatory.
- Identyfikator nazywamy „losowym”, nie „anonimowym” (jest pseudonimowy — ważne dla świadomej zgody).
- Statystyki liczą unikalnych odwiedzających tylko wśród osób ze zgodą.

## D21. Paczki rozmiarowe: automatyczna pełna rozmiarówka, tylko jako skrót *(użytkownik, 2026-10-09)*
- Na karcie produktu wiersz „Pełna rozmiarówka (np. 36–46)” z liczbą paczek: jedno kliknięcie dodaje każdy dostępny
  rozmiar koloru w ilości = liczba paczek. Bez konfiguracji w panelu (bez szablonów proporcji typu 1-2-2-1).
- Pojedyncze rozmiary i ilości nadal dostępne — paczka nie jest minimum zamówienia.
- W zestawieniu przy każdym modelu i kolorze „Wszystkie” ustawia jedną ilość dla wszystkich rozmiarów.
- Tylko frontend: zestawienie i PDF już przechowują ilość przy wariancie, więc backend bez zmian.
- **Możliwe rozszerzenie:** szablony proporcji (np. 1-2-2-1) definiowane w panelu.

