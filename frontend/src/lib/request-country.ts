import "server-only";
import { headers } from "next/headers";

/**
 * Kraj, z którego najpewniej pochodzi żądanie — tylko podpowiedź w formularzu rejestracji (kraj firmy i numer
 * kierunkowy), kupiec zawsze może ją zmienić. Bez zewnętrznych usług geolokalizacji (adres IP nie opuszcza serwera):
 *
 * 1. nagłówek kraju od proxy / CDN, jeśli jest skonfigurowany (Cloudflare `CF-IPCountry`, Vercel, własny
 *    `X-Country-Code` ustawiany przez reverse proxy z bazy GeoIP),
 * 2. region z `Accept-Language` przeglądarki (np. `cs-CZ` → CZ; sam język → jego główny kraj),
 * 3. język strony (`pl` → PL, `cs` → CZ, `uk` → UA).
 */
const COUNTRY_HEADERS = ["cf-ipcountry", "x-vercel-ip-country", "x-country-code"];

/** Język bez regionu → kraj, w którym jest głównym językiem (tylko jednoznaczne przypadki). */
const LANGUAGE_COUNTRY: Record<string, string> = {
  pl: "PL", cs: "CZ", sk: "SK", uk: "UA", de: "DE", lt: "LT", lv: "LV", et: "EE", hu: "HU", ro: "RO", bg: "BG",
  hr: "HR", sl: "SI", it: "IT", fr: "FR", es: "ES", nl: "NL", da: "DK", sv: "SE", fi: "FI", el: "GR", nb: "NO",
};

const LOCALE_COUNTRY: Record<string, string> = { pl: "PL", cs: "CZ", uk: "UA" };

const isCountry = (value: string | null | undefined): value is string =>
  Boolean(value) && /^[A-Z]{2}$/.test(value as string) && value !== "XX" && value !== "T1";

export async function suggestedCountry(locale: string): Promise<string | null> {
  const requestHeaders = await headers();
  for (const name of COUNTRY_HEADERS) {
    const value = requestHeaders.get(name)?.trim().toUpperCase();
    if (isCountry(value)) return value;
  }
  return fromAcceptLanguage(requestHeaders.get("accept-language")) ?? LOCALE_COUNTRY[locale] ?? null;
}

function fromAcceptLanguage(header: string | null): string | null {
  if (!header) return null;
  const tags = header
    .split(",")
    .map((part) => {
      const [tag, ...params] = part.trim().split(";");
      const q = params.map((p) => p.trim()).find((p) => p.startsWith("q="));
      return { tag: tag.trim(), q: q ? Number.parseFloat(q.slice(2)) || 0 : 1 };
    })
    .filter((entry) => entry.tag && entry.tag !== "*")
    .sort((a, b) => b.q - a.q);
  for (const { tag } of tags) {
    const [language, region] = tag.split("-");
    const country = region?.toUpperCase();
    if (isCountry(country)) return country;
    const fromLanguage = LANGUAGE_COUNTRY[language.toLowerCase()];
    if (fromLanguage) return fromLanguage;
  }
  return null;
}
