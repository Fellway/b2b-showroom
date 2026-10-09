"use client";

import { consent } from "./consent";

/**
 * Lekka analityka bez ciasteczek: zdarzenia trafiają paczkami do `/api/v1/analytics/events`.
 * Bez zgody (consent.ts) zdarzenia są w pełni anonimowe — nic nie jest zapisywane w przeglądarce.
 * Po zgodzie: identyfikator sesji (sessionStorage) i trwały identyfikator odwiedzającego (localStorage,
 * odnawiany po 13 miesiącach), dzięki którym statystyki liczą unikalnych odwiedzających.
 */

export type AnalyticsEventType =
  | "PAGE_VIEW"
  | "PRODUCT_VIEW"
  | "SELECTION_ADDED"
  | "SELECTION_REMOVED"
  | "CONTACT_CLICKED";

type QueuedEvent = {
  type: AnalyticsEventType;
  visitorId?: string;
  sessionId?: string;
  language?: string;
  productId?: string;
  variantId?: string;
  path?: string;
  referrerHost?: string;
  properties?: Record<string, string>;
};

const MAX_BATCH = 50;
const FLUSH_DELAY_MS = 2000;
const queue: QueuedEvent[] = [];
let timer: ReturnType<typeof setTimeout> | null = null;

const VISITOR_MAX_AGE_MS = 395 * 24 * 60 * 60 * 1000;

/** Identyfikator odwiedzającego — tylko po zgodzie na analitykę. */
export function visitorId(): string | undefined {
  if (consent.get() !== "granted") return undefined;
  try {
    const stored = JSON.parse(localStorage.getItem("fs-visitor") ?? "null") as { id: string; createdAt: string } | null;
    if (stored && Date.now() - Date.parse(stored.createdAt) < VISITOR_MAX_AGE_MS) return stored.id;
    const id = crypto.randomUUID();
    localStorage.setItem("fs-visitor", JSON.stringify({ id, createdAt: new Date().toISOString() }));
    return id;
  } catch {
    return undefined;
  }
}

function sessionId(): string | undefined {
  if (consent.get() !== "granted") return undefined;
  try {
    let id = sessionStorage.getItem("fs-session");
    if (!id) {
      id = crypto.randomUUID();
      sessionStorage.setItem("fs-session", id);
    }
    return id;
  } catch {
    return undefined;
  }
}

function referrerHost(): string | undefined {
  try {
    if (!document.referrer) return undefined;
    const host = new URL(document.referrer).hostname;
    return host && host !== location.hostname ? host : undefined;
  } catch {
    return undefined;
  }
}

export function track(event: Omit<QueuedEvent, "visitorId" | "sessionId" | "path" | "referrerHost">) {
  if (typeof window === "undefined") return;
  queue.push({
    ...event,
    visitorId: visitorId(),
    sessionId: sessionId(),
    path: location.pathname.slice(0, 512),
    referrerHost: event.type === "PAGE_VIEW" ? referrerHost() : undefined,
  });
  if (queue.length >= MAX_BATCH) {
    flush();
  } else if (!timer) {
    timer = setTimeout(flush, FLUSH_DELAY_MS);
  }
}

function flush() {
  if (timer) {
    clearTimeout(timer);
    timer = null;
  }
  if (queue.length === 0) return;
  const events = queue.splice(0, MAX_BATCH);
  // keepalive: paczka wysyła się także przy zamykaniu karty; błędy analityki nigdy nie przeszkadzają kupcowi
  fetch("/api/v1/analytics/events", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ events }),
    keepalive: true,
  }).catch(() => undefined);
}

if (typeof window !== "undefined") {
  addEventListener("visibilitychange", () => {
    if (document.visibilityState === "hidden") flush();
  });
}
