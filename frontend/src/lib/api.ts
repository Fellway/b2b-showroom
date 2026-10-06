import "server-only";
import type { Category, Facets, Page, ProductCard, ProductDetail, SalesRepresentative } from "./types";

const API = process.env.API_INTERNAL_URL ?? "http://localhost:8080";

/** Katalog zmienia się rzadko — krótkie odświeżanie wystarcza, a strony są lekkie dla backendu. */
const REVALIDATE_SECONDS = 60;

export class NotFoundError extends Error {}

async function get<T>(path: string, lang: string, params?: URLSearchParams): Promise<T> {
  const query = new URLSearchParams(params);
  query.set("lang", lang);
  const response = await fetch(`${API}/api/v1${path}?${query}`, {
    headers: { Accept: "application/json" },
    next: { revalidate: REVALIDATE_SECONDS },
  });
  if (response.status === 404) {
    throw new NotFoundError(path);
  }
  if (!response.ok) {
    throw new Error(`API ${path} responded ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export type CatalogQuery = {
  category?: string;
  colors?: string[];
  sizes?: string[];
  q?: string;
  sort?: string;
  page?: number;
};

export const PAGE_SIZE = 24;

export function fetchProducts(lang: string, query: CatalogQuery) {
  const params = new URLSearchParams();
  if (query.category) params.set("category", query.category);
  query.colors?.forEach((id) => params.append("color", id));
  query.sizes?.forEach((id) => params.append("size", id));
  if (query.q) params.set("q", query.q);
  if (query.sort) params.set("sort", query.sort);
  params.set("page", String(query.page ?? 0));
  params.set("pageSize", String(PAGE_SIZE));
  return get<Page<ProductCard>>("/catalog/products", lang, params);
}

export const fetchProduct = (lang: string, slug: string) =>
  get<ProductDetail>(`/catalog/products/${encodeURIComponent(slug)}`, lang);

export const fetchCategories = (lang: string) => get<Category[]>("/catalog/categories", lang);

export const fetchFacets = (lang: string) => get<Facets>("/catalog/facets", lang);

export const fetchRepresentatives = (lang: string) => get<SalesRepresentative[]>("/contact/representatives", lang);
