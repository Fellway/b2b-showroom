"use client";

import { session } from "./session";

/** Błąd API panelu w kształcie RFC 9457 + stabilny `code` (zob. errors.ts). */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string | undefined,
    readonly detail: string | undefined,
    readonly fieldErrors: { field: string; message: string }[] = [],
    readonly retryAfter?: number,
  ) {
    super(code ?? `HTTP ${status}`);
  }
}

type Body = BodyInit | Record<string, unknown> | unknown[] | null | undefined;

async function send(path: string, init: RequestInit & { json?: Body }, retried = false): Promise<Response> {
  const token = await session.token();
  if (!token) throw new ApiError(401, "auth.unauthenticated", undefined);
  const headers = new Headers(init.headers);
  headers.set("Authorization", `Bearer ${token}`);
  headers.set("Accept", "application/json");
  let body = init.body;
  if (init.json !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(init.json);
  }
  const response = await fetch(`/api/v1/admin${path}`, { ...init, headers, body, credentials: "same-origin" });
  if (response.status === 401 && !retried) {
    // token mógł wygasnąć między sprawdzeniem a żądaniem — jedno odświeżenie i jedna ponowna próba
    if (await session.refresh()) return send(path, init, true);
    session.expire();
  }
  return response;
}

async function toError(response: Response): Promise<ApiError> {
  let problem: { code?: string; detail?: string; errors?: { field: string; message: string }[] } = {};
  try {
    problem = await response.json();
  } catch {
    // brak ciała odpowiedzi
  }
  const retryAfter = Number.parseInt(response.headers.get("Retry-After") ?? "", 10);
  return new ApiError(response.status, problem.code, problem.detail, problem.errors ?? [], Number.isFinite(retryAfter) ? retryAfter : undefined);
}

async function request<T>(path: string, init: RequestInit & { json?: Body } = {}): Promise<T> {
  const response = await send(path, init);
  if (!response.ok) throw await toError(response);
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const adminApi = {
  get: <T>(path: string) => request<T>(path),
  post: <T = void>(path: string, json?: Body) => request<T>(path, { method: "POST", json }),
  put: <T = void>(path: string, json?: Body) => request<T>(path, { method: "PUT", json }),
  delete: (path: string) => request<void>(path, { method: "DELETE" }),
  /** Multipart (np. zdjęcia) — przeglądarka sama ustawia granicę Content-Type. */
  upload: <T>(path: string, form: FormData) => request<T>(path, { method: "POST", body: form }),
  /** Plik wymagający tokenu (np. PDF zestawienia z cenami, D23): fetch → blob → pobranie przez przeglądarkę. */
  async download(path: string, fileName: string) {
    const response = await send(path, {});
    if (!response.ok) throw await toError(response);
    const url = URL.createObjectURL(await response.blob());
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
  },
};

export type Page<T> = { items: T[]; page: number; size: number; totalItems: number; totalPages: number };

export const query = (params: Record<string, string | number | undefined | null>) => {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") search.set(key, String(value));
  }
  const text = search.toString();
  return text ? `?${text}` : "";
};
