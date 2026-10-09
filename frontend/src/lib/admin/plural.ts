const rules = new Intl.PluralRules("pl");

/**
 * Polska odmiana liczebników: `plural(3, "zdjęcie", "zdjęcia", "zdjęć")` → "3 zdjęcia".
 * Formy: jeden / kilka (2–4, 22–24…) / wiele (0, 5–21, 25…).
 */
export function plural(count: number, one: string, few: string, many: string) {
  const form = rules.select(count);
  const word = form === "one" ? one : form === "few" ? few : many;
  return `${count} ${word}`;
}
