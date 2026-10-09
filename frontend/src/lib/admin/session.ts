"use client";

/**
 * Sesja panelu: access token (JWT, 15 min) żyje wyłącznie w pamięci karty; refresh token jest w ciasteczku
 * HttpOnly (`/api/v1/admin/auth`), niedostępnym dla JS. Odświeżanie jest single-flight — backend traktuje dwa
 * równoległe użycia tego samego refresh tokenu jako kradzież i unieważnia sesję.
 */

export type AdminRole = "ADMIN" | "EDITOR";

export type AdminIdentity = { id: string; email: string; displayName: string; role: AdminRole };

type SessionPayload = { accessToken: string; tokenType: string; expiresAt: string; admin: AdminIdentity };

export type SessionState =
  | { status: "loading" }
  | { status: "anonymous" }
  | { status: "authenticated"; admin: AdminIdentity };

const AUTH_HEADERS = { "Content-Type": "application/json", "X-Requested-With": "XMLHttpRequest" };

let accessToken: string | null = null;
let expiresAt = 0;
let state: SessionState = { status: "loading" };
let refreshInFlight: Promise<boolean> | null = null;
const listeners = new Set<() => void>();

function setState(next: SessionState) {
  state = next;
  listeners.forEach((listener) => listener());
}

function accept(payload: SessionPayload) {
  accessToken = payload.accessToken;
  expiresAt = Date.parse(payload.expiresAt);
  setState({ status: "authenticated", admin: payload.admin });
}

function clear() {
  accessToken = null;
  expiresAt = 0;
  setState({ status: "anonymous" });
}

export const session = {
  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
  snapshot: () => state,

  /** Ważny access token; odświeża go z wyprzedzeniem 60 s. `null` = brak sesji. */
  async token(): Promise<string | null> {
    if (accessToken && Date.now() < expiresAt - 60_000) return accessToken;
    return (await session.refresh()) ? accessToken : null;
  },

  refresh(): Promise<boolean> {
    refreshInFlight ??= fetch("/api/v1/admin/auth/refresh", { method: "POST", headers: AUTH_HEADERS, credentials: "same-origin" })
      .then(async (response) => {
        if (!response.ok) {
          clear();
          return false;
        }
        accept((await response.json()) as SessionPayload);
        return true;
      })
      .catch(() => {
        clear();
        return false;
      })
      .finally(() => {
        refreshInFlight = null;
      });
    return refreshInFlight;
  },

  async login(email: string, password: string): Promise<{ ok: true } | { ok: false; status: number; retryAfter?: number }> {
    const response = await fetch("/api/v1/admin/auth/login", {
      method: "POST",
      headers: AUTH_HEADERS,
      credentials: "same-origin",
      body: JSON.stringify({ email, password }),
    });
    if (!response.ok) {
      const retryAfter = Number.parseInt(response.headers.get("Retry-After") ?? "", 10);
      return { ok: false, status: response.status, retryAfter: Number.isFinite(retryAfter) ? retryAfter : undefined };
    }
    accept((await response.json()) as SessionPayload);
    return { ok: true };
  },

  async logout() {
    await fetch("/api/v1/admin/auth/logout", { method: "POST", headers: AUTH_HEADERS, credentials: "same-origin" }).catch(
      () => undefined,
    );
    clear();
  },

  /** Wywoływane przez klienta API, gdy backend odrzuci token (np. konto wyłączone). */
  expire: clear,
};
