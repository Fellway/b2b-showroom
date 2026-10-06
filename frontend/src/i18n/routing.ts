import { defineRouting } from "next-intl/routing";

export const routing = defineRouting({
  locales: ["pl", "en", "cs", "uk"],
  defaultLocale: "pl",
  // polski bez prefiksu (rynek główny), pozostałe języki pod /en, /cs, /uk
  localePrefix: "as-needed",
});

export type Locale = (typeof routing.locales)[number];
