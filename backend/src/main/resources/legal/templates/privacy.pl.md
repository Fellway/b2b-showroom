# Polityka prywatności

> **Wzór do weryfikacji prawnej.** Treść opisuje faktyczne działanie showroomu, ale przed uruchomieniem produkcyjnym musi ją sprawdzić prawnik marki. Pola w nawiasach kwadratowych należy uzupełnić, a tę uwagę usunąć.

## 1. Administrator danych

Administratorem danych osobowych jest [PEŁNA NAZWA FIRMY], [ADRES SIEDZIBY], NIP [NIP], [KRS / CEIDG] — właściciel marki {brand} („my”).

Kontakt w sprawach danych osobowych: [ADRES E-MAIL DO SPRAW DANYCH OSOBOWYCH]. [Jeśli wyznaczono inspektora ochrony danych — jego dane kontaktowe.]

## 2. Czego dotyczy polityka

Showroom hurtowy {brand} to serwis dla klientów biznesowych (B2B): katalog kolekcji, konta kupców, zestawienia PDF i kontakt z opiekunem handlowym. W showroomie nie prowadzimy sprzedaży ani płatności i nie wysyłamy informacji handlowych e-mailem — wysyłamy wyłącznie wiadomości dotyczące konta.

## 3. Jakie dane przetwarzamy, w jakim celu i na jakiej podstawie

### Konto kupca

- **Dane:** adres e-mail, hasło (wyłącznie jako skrót kryptograficzny — nie znamy Twojego hasła), nazwa firmy, kraj, NIP / numer VAT UE, imię i nazwisko osoby kontaktowej, telefon, język, status konta, daty rejestracji, potwierdzenia adresu, decyzji o koncie i ostatniego logowania, licznik nieudanych logowań.
- **Cel:** założenie i prowadzenie konta, sprawdzenie, czy wnioskujący jest klientem hurtowym, udostępnienie cen hurtowych i zestawień, kontakt w sprawie współpracy.
- **Podstawa:** art. 6 ust. 1 lit. b RODO (umowa — regulamin showroomu), a w zakresie danych osoby kontaktowej działającej w imieniu firmy — art. 6 ust. 1 lit. f RODO (nasz prawnie uzasadniony interes: weryfikacja kontrahentów i kontakt handlowy).

### Zestawienia PDF

- **Dane:** dane firmy i osoby kontaktowej z konta w chwili wygenerowania zestawienia (zapisywane razem z zestawieniem), wybrane produkty i ilości orientacyjne, przypisany opiekun handlowy, plik PDF.
- **Cel:** przygotowanie zestawienia na Twoją prośbę i obsługa zapytania przez opiekuna handlowego.
- **Podstawa:** art. 6 ust. 1 lit. b RODO.

### Rejestr akceptacji dokumentów

- **Dane:** który dokument (regulamin, polityka prywatności), w której wersji, kiedy, w jakim języku i w jakiej sytuacji (rejestracja, nowa wersja dokumentu) został zaakceptowany lub przyjęty do wiadomości. Nie zapisujemy adresu IP ani danych przeglądarki.
- **Cel:** wykazanie, na jakich warunkach zawarto umowę i że wykonaliśmy obowiązek informacyjny.
- **Podstawa:** art. 6 ust. 1 lit. c RODO w związku z art. 5 ust. 2 RODO (rozliczalność) oraz art. 6 ust. 1 lit. f RODO.

### Bezpieczeństwo i sesja

- Ciasteczko sesji `showroom_buyer_refresh` (niedostępne dla skryptów, ważne do 30 dni) utrzymuje zalogowanie; w bazie przechowujemy tylko jego skrót. Krótkotrwały token dostępu (15 minut) istnieje wyłącznie w pamięci karty przeglądarki.
- Ograniczamy liczbę żądań z jednego adresu (np. prób logowania). Adres IP jest przy tym używany tylko w pamięci serwera i nie jest zapisywany w bazie ani w statystykach.
- **Podstawa:** art. 6 ust. 1 lit. f RODO (bezpieczeństwo serwisu i kont).

### Statystyki odwiedzin

- **Bez Twojej zgody** zliczamy wyłącznie anonimowe zdarzenia (wyświetlenie strony lub produktu, dodanie produktu do zestawienia, kliknięcie danych kontaktowych) — bez identyfikatorów, adresu IP i danych przeglądarki. Nic nie zapisujemy na Twoim urządzeniu.
- **Za Twoją zgodą** (baner prywatności) zapisujemy w przeglądarce losowy identyfikator odwiedzającego (`fs-visitor`, odnawiany po 13 miesiącach) i identyfikator wizyty (`fs-session`, do zamknięcia karty), dzięki którym liczymy unikalnych odwiedzających.
- Zgodę możesz wycofać w każdej chwili w „Ustawieniach prywatności” w stopce strony — identyfikatory zostaną wtedy usunięte z przeglądarki.
- **Podstawa:** art. 6 ust. 1 lit. a RODO (zgoda) oraz przepisy o przechowywaniu informacji w urządzeniu użytkownika [weryfikacja prawna: właściwy przepis krajowy].

### Kontakt z opiekunem handlowym

Gdy piszesz lub dzwonisz do opiekuna handlowego, przetwarzamy dane z korespondencji, aby odpowiedzieć i prowadzić rozmowy handlowe (art. 6 ust. 1 lit. f RODO).

## 4. Informacje zapisywane w przeglądarce

- `showroom_buyer_refresh` — ciasteczko sesji zalogowanego kupca (niezbędne).
- `fs-buyer-session` — znacznik, że na tym urządzeniu jest zalogowany kupiec (niezbędne).
- `fs-line-sheet` — Twój wybór produktów do zestawienia, bez cen (niezbędne do działania funkcji, z której korzystasz).
- `fs-consent` — Twoja decyzja w sprawie statystyk, ważna 12 miesięcy (niezbędne).
- `fs-visitor`, `fs-session` — identyfikatory statystyk, wyłącznie po wyrażeniu zgody.

Nie używamy ciasteczek reklamowych ani zewnętrznych narzędzi śledzących.

## 5. Odbiorcy danych

- dostawca serwera: [NAZWA DOSTAWCY HOSTINGU, KRAJ] — przechowuje bazę danych, pliki i kopie zapasowe;
- Brevo (Sendinblue SAS, Francja) — doręcza wiadomości dotyczące konta: potwierdzenie adresu, decyzję o koncie, reset hasła, potwierdzenie usunięcia konta;
- [inni odbiorcy, np. biuro rachunkowe, kancelaria — jeśli dotyczy];
- organy publiczne, gdy wymagają tego przepisy.

Podmioty przetwarzające dane w naszym imieniu działają na podstawie umów powierzenia (art. 28 RODO). Nie przekazujemy danych poza Europejski Obszar Gospodarczy [weryfikacja: lokalizacja serwerów dostawców].

## 6. Jak długo przechowujemy dane

- konto i rejestr akceptacji dokumentów — do usunięcia konta (przez Ciebie albo przez nas po zakończeniu współpracy); konta niepotwierdzone i odrzucone — [OKRES];
- zestawienia — [OKRES]. Po usunięciu konta zestawienie zostaje jako ślad oferty (numer, data, produkty, nazwa firmy i numer podatkowy), ale usuwamy z niego dane osoby kontaktowej i plik PDF;
- sesja — do 30 dni; linki z wiadomości e-mail — 48 godzin (potwierdzenie adresu) i 1 godzinę (nowe hasło);
- statystyki — [OKRES];
- kopie zapasowe — do 14 dni;
- dłużej tylko wtedy, gdy wymagają tego przepisy albo jest to potrzebne do ustalenia, dochodzenia lub obrony roszczeń.

## 7. Twoje prawa

Masz prawo dostępu do swoich danych, ich sprostowania, usunięcia, ograniczenia przetwarzania i przenoszenia, prawo sprzeciwu wobec przetwarzania opartego na prawnie uzasadnionym interesie oraz prawo wycofania zgody w dowolnym momencie (bez wpływu na zgodność z prawem przetwarzania przed jej wycofaniem).

W „Moim koncie” możesz samodzielnie:

- pobrać swoje dane (plik JSON: dane konta, historia akceptacji dokumentów, lista zestawień),
- zobaczyć historię zaakceptowanych dokumentów,
- poprawić dane kontaktowe (zmianę nazwy firmy lub numeru podatkowego zgłoś opiekunowi handlowemu),
- usunąć konto.

W pozostałych sprawach napisz na [ADRES E-MAIL DO SPRAW DANYCH OSOBOWYCH]. Masz też prawo wnieść skargę do Prezesa Urzędu Ochrony Danych Osobowych (ul. Stawki 2, 00-193 Warszawa) albo do organu nadzorczego w swoim kraju.

## 8. Dobrowolność podania danych

Podanie danych w formularzu rejestracji jest dobrowolne, ale bez nich nie założymy konta. Nie podejmujemy decyzji wyłącznie w sposób zautomatyzowany — o akceptacji konta decyduje pracownik marki.

## 9. Zmiany polityki

Każda wersja polityki ma numer i datę publikacji, a poprzednie wersje są dostępne na tej stronie. O nowej wersji poinformujemy zalogowanych kupców w showroomie.
