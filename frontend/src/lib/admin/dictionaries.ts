"use client";

import { useQuery } from "@tanstack/react-query";
import { adminApi } from "./api";

/** Kody języków treści katalogu; `pl` jest bazowy (wymagany). */
export const LANGUAGES = ["pl", "en", "cs", "uk"] as const;
export type Language = (typeof LANGUAGES)[number];
export const LANGUAGE_LABEL: Record<Language, string> = { pl: "Polski", en: "Angielski", cs: "Czeski", uk: "Ukraiński" };

export type AdminCategory = {
  id: string;
  parentId: string | null;
  slug: string;
  sortOrder: number;
  visible: boolean;
  translations: Partial<Record<Language, { name: string; description: string | null }>>;
  productCount: number;
  version: number;
};

export type AdminColor = {
  id: string;
  code: string;
  hex: string | null;
  sortOrder: number;
  names: Partial<Record<Language, string>>;
  productCount: number;
  version: number;
};

export type AdminSize = { id: string; code: string; sortOrder: number; variantCount: number; version: number };

/** Klucze zapytań — mutacje słowników unieważniają je, a edytor modelu z nich czyta. */
export const dictionaryKeys = {
  categories: ["admin", "categories"] as const,
  colors: ["admin", "colors"] as const,
  sizes: ["admin", "sizes"] as const,
};

export const useCategories = () =>
  useQuery({ queryKey: dictionaryKeys.categories, queryFn: () => adminApi.get<AdminCategory[]>("/categories") });

export const useColors = () => useQuery({ queryKey: dictionaryKeys.colors, queryFn: () => adminApi.get<AdminColor[]>("/colors") });

export const useSizes = () => useQuery({ queryKey: dictionaryKeys.sizes, queryFn: () => adminApi.get<AdminSize[]>("/sizes") });

export const categoryName = (category: AdminCategory | undefined) => category?.translations.pl?.name ?? category?.slug ?? "—";
export const colorName = (color: AdminColor | undefined) => color?.names.pl ?? color?.code ?? "—";
