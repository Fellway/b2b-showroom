import { ApiError } from "./api";

/**
 * Komunikaty dla administratora po stabilnych kodach błędów API. Kody z backendu nie zmieniają się
 * między wersjami — treść komunikatu backendu (detail) nigdy nie jest pokazywana bezpośrednio.
 */
const MESSAGES: Record<string, string> = {
  "auth.unauthenticated": "Sesja wygasła. Zaloguj się ponownie.",
  "auth.forbidden": "Nie masz uprawnień do tej operacji.",
  "identity.invalid-credentials": "Nieprawidłowy e-mail lub hasło.",
  "identity.password.policy": "Hasło musi mieć od 12 do 128 znaków.",
  "data.concurrent-modification": "Ktoś inny zmienił te dane w międzyczasie. Odśwież stronę i wprowadź zmianę ponownie.",
  "data.conflict": "Zmiana koliduje z istniejącymi danymi.",
  "request.invalid": "Popraw zaznaczone pola.",
  "request.too-large": "Plik jest za duży (maks. 10 MB).",
  "request.rate-limited": "Za dużo prób w krótkim czasie.",
  "request.version-required": "Brak wersji danych — odśwież stronę i spróbuj ponownie.",
  "i18n.default-language-required": "Uzupełnij nazwę po polsku — to język bazowy katalogu.",
  "pricing.non-positive": "Ceny muszą być większe od zera.",
  "pricing.precision": "Ceny mogą mieć najwyżej dwa miejsca po przecinku.",
  "pricing.too-large": "Cena przekracza dopuszczalną wartość.",
  "catalog.product.slug-taken": "Ten adres (slug) ma już inny model.",
  "catalog.product.style-code-taken": "Ten kod modelu ma już inny model.",
  "catalog.product.slug": "Adres może zawierać małe litery, cyfry i pojedyncze myślniki.",
  "catalog.product.publish-without-variants": "Aby opublikować model, dodaj przynajmniej jeden aktywny wariant.",
  "catalog.product.publish-archived": "Zarchiwizowany model najpierw przywróć do szkicu.",
  "catalog.product.last-active-variant": "Opublikowany model musi mieć aktywny wariant. Najpierw ukryj model.",
  "catalog.product.hide-not-published": "Ukryć można tylko opublikowany model.",
  "catalog.product.color-duplicate": "Ten kolor jest już dodany do modelu.",
  "catalog.variant.size-duplicate": "Ten kolor ma już wariant w tym rozmiarze.",
  "catalog.variant.sku-taken": "Ten SKU jest już użyty.",
  "catalog.variant.ean-taken": "Ten EAN jest już użyty.",
  "catalog.ean.format": "EAN musi mieć 8, 12, 13 lub 14 cyfr.",
  "catalog.ean.check-digit": "Nieprawidłowa cyfra kontrolna EAN.",
  "catalog.sku.format": "SKU może zawierać litery, cyfry i znaki . _ / - (maks. 64).",
  "catalog.image.unsupported-format": "Obsługiwane formaty: JPEG, PNG, WebP, AVIF.",
  "catalog.image.empty": "Plik jest pusty.",
  "catalog.image.too-large": "Zdjęcie przekracza 10 MB.",
  "catalog.category.in-use": "Kategoria ma podkategorie lub modele — najpierw je przenieś.",
  "catalog.category.slug-taken": "Ten adres ma już inna kategoria.",
  "catalog.category.cycle": "Kategorii nie można przenieść pod nią samą ani pod jej podkategorię.",
  "catalog.color.in-use": "Kolor jest używany w modelach.",
  "catalog.color.code-taken": "Ten kod koloru już istnieje.",
  "catalog.size.in-use": "Rozmiar jest używany w wariantach.",
  "catalog.size.code-taken": "Ten rozmiar już istnieje.",
};

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429) {
      return `Za dużo prób w krótkim czasie.${error.retryAfter ? ` Spróbuj ponownie za ${error.retryAfter} s.` : ""}`;
    }
    if (error.code && MESSAGES[error.code]) return MESSAGES[error.code];
    if (error.status === 404) return "Nie znaleziono — element mógł zostać usunięty.";
    if (error.status === 403) return MESSAGES["auth.forbidden"];
    if (error.status >= 500) return "Błąd serwera. Spróbuj ponownie za chwilę.";
    return "Nie udało się zapisać zmian.";
  }
  return "Brak połączenia z serwerem.";
}

/** Błędy walidacji pól (Bean Validation) jako mapa pole → komunikat. */
export function fieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) return {};
  return Object.fromEntries(error.fieldErrors.map((e) => [e.field, "Popraw to pole."]));
}
