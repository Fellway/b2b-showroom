"use client";

import type { BuyerAccount, BuyerStatus } from "@/lib/types";

/**
 * Sesja kupca (D23) — ta sama mechanika co w panelu (`lib/admin/session.ts`): access token (JWT, 15 min) wyłącznie
 * w pamięci karty, refresh token w ciasteczku HttpOnly `showroom_buyer_refresh` (`/api/v1/buyer/auth`), odświeżanie
 * single-flight (dwa równoległe użycia tego samego refresh tokenu backend traktuje jako kradzież).
 *
 * Żeby niezalogowany gość nie wysyłał przy każdym wejściu próby odświeżenia, po zalogowaniu zapisujemy w localStorage
 * znacznik „mam sesję” (bez żadnych danych) — tylko wtedy po przeładowaniu strony odtwarzamy sesję.
 */

type SessionPayload = { accessToken: string; tokenType: string; expiresAt: string; buyer: BuyerAccount };

export type BuyerSessionState =
  | { status: "loading" }
  | { status: "anonymous" }
  | { status: "authenticated"; buyer: BuyerAccount };

export type LoginResult =
  | { ok: true }
  | { ok: false; reason: "credentials" | "network" | "unknown" }
  | { ok: false; reason: "inactive"; accountStatus: BuyerStatus }
  | { ok: false; reason: "rate-limited"; retryAfter: number };

const AUTH = "/api/v1/buyer/auth";
const AUTH_HEADERS = { "Content-Type": "application/json", "X-Requested-With": "XMLHttpRequest" };
const MARKER = "fs-buyer-session";

let accessToken: string | null = null;
let expiresAt = 0;
let state: BuyerSessionState = { status: "loading" };
let refreshInFlight: Promise<boolean> | null = null;
const listeners = new Set<() => void>();

function setState(next: BuyerSessionState) {
  state = next;
  listeners.forEach((listener) => listener());
}

function marker(present: boolean) {
  try {
    if (present) localStorage.setItem(MARKER, "1");
    else localStorage.removeItem(MARKER);
  } catch {
    // tryb prywatny / zablokowany storage — sesja i tak działa do przeładowania
  }
}

function accept(payload: SessionPayload) {
  accessToken = payload.accessToken;
  expiresAt = Date.parse(payload.expiresAt);
  marker(true);
  setState({ status: "authenticated", buyer: payload.buyer });
}

function clear() {
  accessToken = null;
  expiresAt = 0;
  marker(false);
  setState({ status: "anonymous" });
}

const retryAfterOf = (response: Response) => {
  const seconds = Number.parseInt(response.headers.get("Retry-After") ?? "", 10);
  return Number.isFinite(seconds) ? seconds : 60;
};

export const buyerSession = {
  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
  snapshot: () => state,

  /** Po załadowaniu strony: odtwarza sesję z ciasteczka tylko, gdy kupiec był zalogowany w tej przeglądarce. */
  restore() {
    if (state.status !== "loading") return;
    let known = false;
    try {
      known = localStorage.getItem(MARKER) === "1";
    } catch {
      known = false;
    }
    if (known) void buyerSession.refresh();
    else setState({ status: "anonymous" });
  },

  /** Ważny access token; odświeża go z wyprzedzeniem 60 s. `null` = brak sesji. */
  async token(): Promise<string | null> {
    if (accessToken && Date.now() < expiresAt - 60_000) return accessToken;
    if (state.status === "anonymous") return null;
    return (await buyerSession.refresh()) ? accessToken : null;
  },

  refresh(): Promise<boolean> {
    refreshInFlight ??= fetch(`${AUTH}/refresh`, { method: "POST", headers: AUTH_HEADERS, credentials: "same-origin" })
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

  async login(email: string, password: string): Promise<LoginResult> {
    let response: Response;
    try {
      response = await fetch(`${AUTH}/login`, {
        method: "POST",
        headers: AUTH_HEADERS,
        credentials: "same-origin",
        body: JSON.stringify({ email, password }),
      });
    } catch {
      return { ok: false, reason: "network" };
    }
    if (response.ok) {
      accept((await response.json()) as SessionPayload);
      return { ok: true };
    }
    if (response.status === 429) return { ok: false, reason: "rate-limited", retryAfter: retryAfterOf(response) };
    if (response.status === 401) return { ok: false, reason: "credentials" };
    if (response.status === 403) {
      const problem = (await response.json().catch(() => ({}))) as { status?: BuyerStatus };
      if (problem.status) return { ok: false, reason: "inactive", accountStatus: problem.status };
    }
    return { ok: false, reason: "unknown" };
  },

  /** Zmiana hasła zwraca nową sesję (pozostałe są zakończone). */
  adopt(payload: unknown) {
    accept(payload as SessionPayload);
  },

  /** Aktualizacja danych konta w stanie sesji po edycji profilu. */
  update(buyer: BuyerAccount) {
    if (state.status === "authenticated") setState({ status: "authenticated", buyer });
  },

  async logout() {
    await fetch(`${AUTH}/logout`, { method: "POST", headers: AUTH_HEADERS, credentials: "same-origin" }).catch(() => undefined);
    clear();
  },

  /** Wywoływane przez klienta API, gdy backend odrzuci token (np. konto zawieszone). */
  expire: clear,
};

/** Publiczne akcje kont bez logowania (rejestracja, potwierdzenie, reset hasła). */
export async function buyerAuthPost(path: string, body: unknown, lang?: string): Promise<Response> {
  return fetch(`${AUTH}${path}${lang ? `?lang=${lang}` : ""}`, {
    method: "POST",
    headers: AUTH_HEADERS,
    credentials: "same-origin",
    body: JSON.stringify(body),
  });
}
