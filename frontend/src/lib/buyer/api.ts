"use client";

import { useEffect, useMemo, useState, useSyncExternalStore } from "react";
import type { Problem, ProductPricing } from "@/lib/types";
import { buyerSession, type BuyerSessionState } from "./session";

/** Błąd API kupca: status + stabilny `code` (RFC 9457). */
export class BuyerApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem: Problem,
    readonly retryAfter?: number,
  ) {
    super(problem.code ?? `HTTP ${status}`);
  }
}

async function send(path: string, init: RequestInit, retried = false): Promise<Response> {
  const token = await buyerSession.token();
  if (!token) throw new BuyerApiError(401, { status: 401, code: "auth.unauthenticated" });
  const headers = new Headers(init.headers);
  headers.set("Authorization", `Bearer ${token}`);
  const response = await fetch(`/api/v1/buyer${path}`, { ...init, headers, credentials: "same-origin" });
  if (response.status === 401 && !retried) {
    // token mógł wygasnąć między sprawdzeniem a żądaniem — jedno odświeżenie i jedna ponowna próba
    if (await buyerSession.refresh()) return send(path, init, true);
    buyerSession.expire();
  }
  return response;
}

async function toError(response: Response): Promise<BuyerApiError> {
  let problem: Problem = { status: response.status };
  try {
    problem = { ...((await response.json()) as Problem), status: response.status };
  } catch {
    // brak ciała odpowiedzi
  }
  const retryAfter = Number.parseInt(response.headers.get("Retry-After") ?? "", 10);
  return new BuyerApiError(response.status, problem, Number.isFinite(retryAfter) ? retryAfter : undefined);
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await send(path, { ...init, headers: { Accept: "application/json", ...init.headers } });
  if (!response.ok) throw await toError(response);
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

const json = (method: string, body: unknown): RequestInit => ({
  method,
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(body),
});

export const buyerApi = {
  get: <T>(path: string) => request<T>(path),
  post: <T = void>(path: string, body?: unknown) => request<T>(path, json("POST", body ?? {})),
  put: <T = void>(path: string, body: unknown) => request<T>(path, json("PUT", body)),

  /**
   * Pobranie PDF z tokenem (link bez nagłówka Authorization nie zadziała — PDF zawiera ceny, D23):
   * fetch → blob → tymczasowy link z atrybutem download.
   */
  async download(apiPath: string, fileName: string) {
    const response = await send(apiPath.replace(/^\/api\/v1\/buyer/, ""), {});
    if (!response.ok) throw await toError(response);
    saveBlob(await response.blob(), fileName);
  },
};

export function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

const LOADING: BuyerSessionState = { status: "loading" };

export function useBuyerSession(): BuyerSessionState {
  return useSyncExternalStore(buyerSession.subscribe, buyerSession.snapshot, () => LOADING);
}

// ------------------------------------------------------------------ ceny (D23)

const MAX_PER_REQUEST = 100;
const cache = new Map<string, ProductPricing | null>();

/** Zapytania w toku po kluczu — dwa komponenty (albo podwójny efekt w StrictMode) nie pytają o to samo dwa razy. */
const inFlight = new Map<string, Promise<void>>();

function loadPricing(ids: string[]): Promise<void> {
  const key = ids.join(",");
  let pending = inFlight.get(key);
  if (!pending) {
    pending = fetchMissing(ids).finally(() => inFlight.delete(key));
    inFlight.set(key, pending);
  }
  return pending;
}

async function fetchMissing(ids: string[]) {
  const missing = ids.filter((id) => !cache.has(id));
  for (let i = 0; i < missing.length; i += MAX_PER_REQUEST) {
    const chunk = missing.slice(i, i + MAX_PER_REQUEST);
    const params = new URLSearchParams();
    chunk.forEach((id) => params.append("product", id));
    const result = await buyerApi.get<ProductPricing[]>(`/catalog/pricing?${params}`);
    chunk.forEach((id) => cache.set(id, null)); // produkt niewidoczny = brak cen, bez ponawiania
    result.forEach((pricing) => cache.set(pricing.productId, pricing));
  }
}

buyerSession.subscribe(() => {
  // ceny należą do zalogowanego kupca — po wylogowaniu nie mogą zostać w pamięci karty
  if (buyerSession.snapshot().status !== "authenticated") cache.clear();
});

export type PricingState =
  | { status: "anonymous" }
  | { status: "loading" }
  | { status: "ready"; byProduct: Map<string, ProductPricing> }
  | { status: "error" };

/**
 * Ceny i EAN wskazanych produktów dla zalogowanego kupca. Publiczne strony katalogu są współdzielone i cache'owane,
 * dlatego dane handlowe dociąga dopiero przeglądarka — jednym żądaniem na stronę.
 */
export function usePricing(productIds: string[]): PricingState {
  const session = useBuyerSession();
  const key = useMemo(() => [...new Set(productIds)].sort().join(","), [productIds]);
  const [loaded, setLoaded] = useState<{ key: string; failed: boolean } | null>(null);
  const authenticated = session.status === "authenticated";

  useEffect(() => {
    if (!authenticated || key === "") return;
    let cancelled = false;
    loadPricing(key.split(","))
      .then(() => !cancelled && setLoaded({ key, failed: false }))
      .catch(() => !cancelled && setLoaded({ key, failed: true }));
    return () => {
      cancelled = true;
    };
  }, [authenticated, key]);

  if (session.status === "loading") return { status: "loading" };
  if (!authenticated) return { status: "anonymous" };
  if (key === "") return { status: "ready", byProduct: new Map() };
  if (loaded?.key !== key) return { status: "loading" };
  if (loaded.failed) return { status: "error" };
  const byProduct = new Map<string, ProductPricing>();
  key.split(",").forEach((id) => {
    const pricing = cache.get(id);
    if (pricing) byProduct.set(id, pricing);
  });
  return { status: "ready", byProduct };
}
