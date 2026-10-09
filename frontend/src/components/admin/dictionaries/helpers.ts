import { LANGUAGES, type Language } from "@/lib/admin/dictionaries";

const POLISH: Record<string, string> = { ą: "a", ć: "c", ę: "e", ł: "l", ń: "n", ó: "o", ś: "s", ź: "z", ż: "z" };

/** Adres URL z nazwy: małe litery, cyfry i pojedyncze myślniki (zgodnie z walidacją backendu). */
export function slugify(text: string): string {
  return text
    .toLowerCase()
    .replace(/[ąćęłńóśźż]/g, (c) => POLISH[c] ?? c)
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 120);
}

/** Języki bez tłumaczenia (tłumaczenia wklejane z zewnątrz — panel pokazuje braki). */
export function missingLanguages(present: Partial<Record<Language, unknown>>): Language[] {
  return LANGUAGES.filter((code) => !present[code]);
}


export const missingText = (codes: string[]) => (codes.length ? `brak: ${codes.map((c) => c.toUpperCase()).join(", ")}` : "komplet");

/** Liczba całkowita ≥ 0 z pola tekstowego; `null`, gdy niepoprawna. */
export function parseOrder(value: string): number | null {
  if (!/^\d{1,6}$/.test(value.trim())) return null;
  return Number(value.trim());
}
