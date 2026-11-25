import {
  getCountries,
  getCountryCallingCode,
  isPossiblePhoneNumber,
  parsePhoneNumberFromString,
  type CountryCode,
} from "libphonenumber-js/min";

/**
 * Numery telefonów kupców: kraj (numer kierunkowy) wybierany z listy + numer krajowy. Zapisujemy postać
 * międzynarodową (np. `+48 600 111 222`), żeby opiekun handlowy zawsze wiedział, dokąd dzwoni.
 */

export type PhoneCountry = { code: CountryCode; name: string; dialCode: string };

/** Rynki marki na początku listy krajów (jak przy kraju firmy). */
const PRIMARY: CountryCode[] = ["PL", "CZ", "SK", "UA"];

const SUPPORTED = new Set<string>(getCountries());

export const isSupportedCountry = (code: string | null | undefined): code is CountryCode =>
  Boolean(code) && SUPPORTED.has(code as string);

/** Wszystkie kraje z numerem kierunkowym: najpierw rynki marki, potem alfabetycznie w języku strony. */
export function phoneCountries(locale: string): PhoneCountry[] {
  const names = new Intl.DisplayNames([locale], { type: "region" });
  const collator = new Intl.Collator(locale);
  const toCountry = (code: CountryCode): PhoneCountry => ({
    code,
    name: names.of(code) ?? code,
    dialCode: `+${getCountryCallingCode(code)}`,
  });
  const rest = getCountries()
    .filter((code) => !PRIMARY.includes(code))
    .map(toCountry)
    .sort((a, b) => collator.compare(a.name, b.name));
  return [...PRIMARY.map(toCountry), ...rest];
}

/** Numer krajowy + kraj → postać międzynarodowa (`+48 600 111 222`); `null`, gdy numer nie może istnieć. */
export function toInternational(country: CountryCode, national: string): string | null {
  const digits = national.replace(/[^\d]/g, "");
  if (digits.length === 0) return null;
  const full = `+${getCountryCallingCode(country)}${digits.replace(/^0+(?=\d{6,})/, "")}`;
  if (!isPossiblePhoneNumber(full)) return null;
  return parsePhoneNumberFromString(full)?.formatInternational() ?? full;
}

/**
 * Rozkłada zapisany numer na kraj i numer krajowy (do edycji w „Moim koncie”). Numery bez prefiksu `+`
 * (sprzed tej zmiany) traktujemy jako krajowe w kraju domyślnym.
 */
export function splitPhone(value: string, fallback: CountryCode): { country: CountryCode; national: string } {
  const parsed = value.trim().startsWith("+") ? parsePhoneNumberFromString(value) : undefined;
  if (parsed?.country) {
    return { country: parsed.country, national: parsed.formatNational() };
  }
  return { country: fallback, national: value.trim() };
}
