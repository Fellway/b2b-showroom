"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ProductStatus } from "@/components/admin/ui/status-label";
import { adminApi, query, type Page } from "./api";

export type ProductListItem = {
  id: string;
  slug: string;
  styleCode: string;
  name: string;
  categoryName: string;
  status: ProductStatus;
  price: { wholesale: number; retail: number; currency: string; markup: number };
  colorCount: number;
  variantCount: number;
  activeVariantCount: number;
  missingLanguages: string[];
  cover: { id: string; url: string; width: number | null; height: number | null } | null;
  updatedAt: string;
  version: number;
};

export type ProductListFilter = { q?: string; status?: string; categoryId?: string; page: number };

/** Pełny model z GET /products/{id} — potrzebny, bo PUT zastępuje wszystkie dane podstawowe naraz. */
type ProductDetailsResponse = {
  categoryId: string;
  slug: string;
  styleCode: string;
  price: { wholesale: number; retail: number; currency: string };
  featured: boolean;
  sortOrder: number;
  translations: Record<string, { name: string; description: string | null; materialComposition: string | null; careInstructions: string | null }>;
  version: number;
};

export const PAGE_SIZE = 25;

export const productListKey = ["admin", "products", "list"] as const;

export function useProductList(filter: ProductListFilter) {
  return useQuery({
    queryKey: [...productListKey, filter],
    queryFn: () =>
      adminApi.get<Page<ProductListItem>>(
        `/products${query({ q: filter.q, status: filter.status, categoryId: filter.categoryId, page: filter.page, size: PAGE_SIZE })}`,
      ),
    placeholderData: keepPreviousData,
  });
}

export type StatusTransition = "PUBLISH" | "HIDE" | "ARCHIVE" | "RESTORE_TO_DRAFT";

export function useChangeStatus() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, transition, version }: { id: string; transition: StatusTransition; version: number }) =>
      adminApi.post(`/products/${id}/status`, { transition, version }),
    onSettled: () => client.invalidateQueries({ queryKey: ["admin", "products"] }),
  });
}

/**
 * Szybka zmiana ceny hurtowej z listy. Endpoint PUT przyjmuje komplet danych podstawowych, więc najpierw
 * pobieramy aktualny model (z tłumaczeniami) i odsyłamy go bez zmian poza ceną — z jego bieżącą wersją.
 */
export function useUpdateWholesale() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, wholesale }: { id: string; wholesale: number }) => {
      const product = await adminApi.get<ProductDetailsResponse>(`/products/${id}`);
      await adminApi.put(`/products/${id}`, {
        categoryId: product.categoryId,
        slug: product.slug,
        styleCode: product.styleCode,
        wholesalePrice: wholesale,
        retailPrice: product.price.retail,
        currency: product.price.currency,
        featured: product.featured,
        sortOrder: product.sortOrder,
        translations: product.translations,
        version: product.version,
      });
    },
    onSettled: () => client.invalidateQueries({ queryKey: ["admin", "products"] }),
  });
}

/** Kwota wpisana po polsku („159,5”, „1 290,00”) → liczba z dwoma miejscami; `null`, gdy niepoprawna. */
export function parseMoney(input: string): number | null {
  const normalized = input.replace(/\s/g, "").replace(",", ".");
  if (!/^\d{1,10}(\.\d{1,2})?$/.test(normalized)) return null;
  const value = Number(normalized);
  return value > 0 ? value : null;
}

export const formatPln = (value: number, currency = "PLN") =>
  new Intl.NumberFormat("pl-PL", { style: "currency", currency }).format(value);

export const formatDate = (iso: string) =>
  new Date(iso).toLocaleDateString("pl-PL", { day: "numeric", month: "short", year: "numeric" });
