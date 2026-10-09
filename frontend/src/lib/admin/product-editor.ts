"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { productListKey } from "./product-list";

export { productListKey };
import { adminApi } from "./api";
import { LANGUAGES, type Language } from "./dictionaries";
import type { ProductStatus } from "@/components/admin/ui/status-label";

/** Kształty `/api/v1/admin/products/{id}` (AdminProductResponses.ProductResponse). */
export type ProductTranslation = {
  name: string;
  description: string | null;
  materialComposition: string | null;
  careInstructions: string | null;
};

export type AdminProduct = {
  id: string;
  categoryId: string;
  slug: string;
  styleCode: string;
  status: ProductStatus;
  price: { wholesale: number; retail: number; currency: string; markup: number };
  featured: boolean;
  sortOrder: number;
  publishedAt: string | null;
  translations: Partial<Record<Language, ProductTranslation>>;
  colors: { id: string; code: string; name: string; hex: string | null }[];
  variants: { id: string; colorId: string; sizeId: string; size: string; sku: string; ean: string; active: boolean }[];
  images: { id: string; colorId: string | null; url: string; width: number | null; height: number | null }[];
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type StatusTransition = "PUBLISH" | "HIDE" | "ARCHIVE" | "RESTORE_TO_DRAFT";

export const productKey = (id: string) => ["admin", "product", id] as const;
/** Prefiks listy modeli (strona listy należy do innego modułu — unieważniamy cały prefiks). */

export const useProduct = (id: string) =>
  useQuery({ queryKey: productKey(id), queryFn: () => adminApi.get<AdminProduct>(`/products/${id}`) });

/** Po każdej zmianie dziecka (kolor, wariant, zdjęcie) backend podbija wersję modelu — odświeżamy całość. */
export function useProductRefresh(id: string) {
  const client = useQueryClient();
  return async () => {
    await client.invalidateQueries({ queryKey: productKey(id) });
    void client.invalidateQueries({ queryKey: productListKey });
  };
}

// ------------------------------------------------------------------ formularz danych i tłumaczeń

export type TranslationDraft = { name: string; description: string; materialComposition: string; careInstructions: string };

export type ProductDraft = {
  categoryId: string;
  slug: string;
  /** dopóki administrator nie edytuje adresu ręcznie, wynika on z polskiej nazwy */
  slugTouched: boolean;
  styleCode: string;
  wholesale: string;
  retail: string;
  featured: boolean;
  sortOrder: string;
  translations: Record<Language, TranslationDraft>;
};

const EMPTY_TRANSLATION: TranslationDraft = { name: "", description: "", materialComposition: "", careInstructions: "" };

export function emptyDraft(): ProductDraft {
  return {
    categoryId: "",
    slug: "",
    slugTouched: false,
    styleCode: "",
    wholesale: "",
    retail: "",
    featured: false,
    sortOrder: "0",
    translations: Object.fromEntries(LANGUAGES.map((l) => [l, { ...EMPTY_TRANSLATION }])) as Record<Language, TranslationDraft>,
  };
}

const decimalText = (value: number) => value.toFixed(2).replace(".", ",");

export function draftFrom(product: AdminProduct): ProductDraft {
  return {
    categoryId: product.categoryId,
    slug: product.slug,
    slugTouched: true,
    styleCode: product.styleCode,
    wholesale: decimalText(product.price.wholesale),
    retail: decimalText(product.price.retail),
    featured: product.featured,
    sortOrder: String(product.sortOrder),
    translations: Object.fromEntries(
      LANGUAGES.map((language) => {
        const t = product.translations[language];
        return [
          language,
          t
            ? {
                name: t.name ?? "",
                description: t.description ?? "",
                materialComposition: t.materialComposition ?? "",
                careInstructions: t.careInstructions ?? "",
              }
            : { ...EMPTY_TRANSLATION },
        ];
      }),
    ) as Record<Language, TranslationDraft>,
  };
}

/** Porównanie bez pola technicznego `slugTouched`. */
export function sameDraft(a: ProductDraft, b: ProductDraft) {
  const strip = (draft: ProductDraft) => JSON.stringify({ ...draft, slugTouched: undefined });
  return strip(a) === strip(b);
}

/** Kwota z polskim lub angielskim separatorem dziesiętnym; `null`, gdy niepoprawna lub ponad 2 miejsca po przecinku. */
export function parseMoney(text: string): number | null {
  const normalized = text.trim().replace(/\s/g, "").replace(",", ".");
  if (!/^\d{1,10}(\.\d{1,2})?$/.test(normalized)) return null;
  const value = Number(normalized);
  return value > 0 ? value : null;
}

export const isTranslationFilled = (t: TranslationDraft) => t.name.trim() !== "";

export type DraftErrors = Partial<Record<"categoryId" | "name" | "slug" | "styleCode" | "wholesale" | "retail" | "sortOrder", string>> & {
  translations?: Partial<Record<Language, string>>;
};

const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;

export function validateDraft(draft: ProductDraft): DraftErrors {
  const errors: DraftErrors = {};
  if (!draft.categoryId) errors.categoryId = "Wybierz kategorię.";
  if (!draft.translations.pl.name.trim()) errors.name = "Podaj nazwę po polsku.";
  if (!SLUG_PATTERN.test(draft.slug)) errors.slug = "Adres: małe litery, cyfry i pojedyncze myślniki.";
  if (!draft.styleCode.trim()) errors.styleCode = "Podaj kod modelu.";
  if (parseMoney(draft.wholesale) === null) errors.wholesale = "Podaj cenę większą od zera, np. 149,00.";
  if (parseMoney(draft.retail) === null) errors.retail = "Podaj cenę większą od zera, np. 349,00.";
  if (!/^\d{1,6}$/.test(draft.sortOrder.trim())) errors.sortOrder = "Liczba całkowita, 0 lub więcej.";
  for (const language of LANGUAGES.filter((l) => l !== "pl")) {
    const t = draft.translations[language];
    const hasOther = t.description.trim() || t.materialComposition.trim() || t.careInstructions.trim();
    if (hasOther && !t.name.trim()) {
      errors.translations = { ...errors.translations, [language]: "Uzupełnij nazwę — bez niej tłumaczenie nie zostanie zapisane." };
    }
  }
  return errors;
}

export const hasErrors = (errors: DraftErrors) =>
  Object.entries(errors).some(([key, value]) => (key === "translations" ? Object.keys(value ?? {}).length > 0 : Boolean(value)));

const optional = (text: string) => (text.trim() === "" ? null : text.trim());

/** Ciało POST/PUT `/products` — język bez nazwy jest pomijany (API odrzuca puste nazwy). */
export function toPayload(draft: ProductDraft, version?: number) {
  const translations = Object.fromEntries(
    LANGUAGES.filter((language) => isTranslationFilled(draft.translations[language])).map((language) => {
      const t = draft.translations[language];
      return [
        language,
        {
          name: t.name.trim(),
          description: optional(t.description),
          materialComposition: optional(t.materialComposition),
          careInstructions: optional(t.careInstructions),
        },
      ];
    }),
  );
  return {
    categoryId: draft.categoryId,
    slug: draft.slug,
    styleCode: draft.styleCode.trim().toUpperCase(),
    wholesalePrice: parseMoney(draft.wholesale),
    retailPrice: parseMoney(draft.retail),
    currency: "PLN",
    featured: draft.featured,
    sortOrder: Number.parseInt(draft.sortOrder, 10) || 0,
    translations,
    ...(version === undefined ? {} : { version }),
  };
}

// ------------------------------------------------------------------ pomocnicze

const POLISH: Record<string, string> = { ą: "a", ć: "c", ę: "e", ł: "l", ń: "n", ó: "o", ś: "s", ź: "z", ż: "z" };

/** Adres URL z nazwy: polskie znaki transliterowane, reszta diakrytyków usunięta, myślniki zamiast spacji. */
export function slugify(text: string) {
  return text
    .toLowerCase()
    .replace(/[ąćęłńóśźż]/g, (ch) => POLISH[ch] ?? ch)
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 160)
    .replace(/-+$/g, "");
}

/** GTIN-8/12/13/14 z cyfrą kontrolną GS1 (ta sama reguła co w domenie backendu). */
export function isValidGtin(value: string) {
  const digits = value.trim();
  if (!/^(\d{8}|\d{12,14})$/.test(digits)) return false;
  let sum = 0;
  const last = digits.length - 1;
  for (let i = last - 1, position = 0; i >= 0; i--, position++) {
    const digit = digits.charCodeAt(i) - 48;
    sum += position % 2 === 0 ? digit * 3 : digit;
  }
  return (10 - (sum % 10)) % 10 === digits.charCodeAt(last) - 48;
}

export function gtinProblem(value: string): string | null {
  const digits = value.trim();
  if (!digits) return "Podaj EAN.";
  if (!/^(\d{8}|\d{12,14})$/.test(digits)) return "EAN: 8, 12, 13 lub 14 cyfr.";
  return isValidGtin(digits) ? null : "Nieprawidłowa cyfra kontrolna.";
}

/** Proponowany SKU: kod modelu bez myślników, kod koloru, rozmiar — np. FS2601-NAVY-38. */
export const suggestSku = (styleCode: string, colorCode: string, size: string) =>
  [styleCode.replace(/-/g, ""), colorCode, size].join("-").toUpperCase().replace(/[^A-Z0-9._/-]/g, "");

export const formatMoney = (value: number) =>
  new Intl.NumberFormat("pl-PL", { style: "currency", currency: "PLN" }).format(value);
