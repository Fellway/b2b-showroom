const intlLocales: Record<string, string> = { pl: "pl-PL", en: "en-GB", cs: "cs-CZ", uk: "uk-UA" };

export const intlLocale = (lang: string) => intlLocales[lang] ?? "pl-PL";

export function formatMoney(value: number, currency: string, lang: string) {
  return new Intl.NumberFormat(intlLocale(lang), { style: "currency", currency }).format(value);
}

export function formatMarkup(value: number, lang: string) {
  return "×" + new Intl.NumberFormat(intlLocale(lang), { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
}

export function formatNumber(value: number, lang: string) {
  return new Intl.NumberFormat(intlLocale(lang)).format(value);
}
