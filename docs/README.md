# Dokumentacja projektu — Fabian's Style B2B Showroom

Interaktywny katalog hurtowy (lookbook) marki odzieży damskiej Fabian's Style: kupcy przeglądają kolekcję,
budują zestawienie wariantów i pobierają PDF z cenami hurtowymi, MSRP i kodami EAN; marka zarządza ofertą w panelu.

## Gdzie jest jaka wiedza

| Pytanie | Dokument |
|---|---|
| Od czego zacząć, zasady pracy, konwencje kodu, bezpieczeństwo (czytane automatycznie przez Claude Code) | [`../CLAUDE.md`](../CLAUDE.md) |
| **Dlaczego** coś jest zrobione tak, a nie inaczej | [decisions.md](decisions.md) — rejestr decyzji D1–D24 |
| Jak części systemu współpracują, przepływy (zestawienie PDF, sesja panelu, zdjęcia, zgoda) | [architecture.md](architecture.md) |
| Jak uruchomić, przetestować, pułapki środowiska | [development.md](development.md) |
| Wdrożenie na serwer, aktualizacja, kopie zapasowe, odtwarzanie | [deployment.md](deployment.md) |
| Gdzie jesteśmy, co dalej, lista przed produkcją, na co czekamy od marki | [roadmap.md](roadmap.md) |
| Jak powstał projekt UI/UX i jak prowadzić kolejne prace projektowe | [design-process.md](design-process.md) |
| Produkt: użytkownicy, marka, zasady | [`../PRODUCT.md`](../PRODUCT.md) |
| System wizualny: tokeny, komponenty, zasady | [`../DESIGN.md`](../DESIGN.md) |
| Backend: uruchomienie, konfiguracja, API, bezpieczeństwo, PDF | [`../backend/README.md`](../backend/README.md) |
| Frontend: uruchomienie, katalog, panel | [`../frontend/README.md`](../frontend/README.md) |
| Historia zmian | `git log` (każdy etap opisany w commicie) |

## Zasada utrzymania

Dokumentacja żyje razem z kodem: każda nowa decyzja trafia do `decisions.md`, zmiana statusu do `roadmap.md`
i tabeli etapów w `CLAUDE.md`, zmiana systemu wizualnego do `DESIGN.md`. Nie powielamy treści — dokumenty
odsyłają do siebie nawzajem.
