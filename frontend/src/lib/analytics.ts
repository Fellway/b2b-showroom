"use client";

/**
 * Lekka analityka bez ciasteczek: zdarzenia trafiają paczkami do `/api/v1/analytics/events`.
 * Identyfikator sesji żyje tylko w sessionStorage karty. Trwały identyfikator odwiedzającego wymaga
 * decyzji o zgodzie (ePrivacy) — do tego czasu nie jest wysyłany.
 */

export type AnalyticsEventType =
  | "PAGE_VIEW"
  | "PRODUCT_VIEW"
  | "SELECTION_ADDED"
  | "SELECTION_REMOVED"
  | "CONTACT_CLICKED";

type QueuedEvent = {
  type: AnalyticsEventType;
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

function sessionId(): string | undefined {
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

export function track(event: Omit<QueuedEvent, "sessionId" | "path" | "referrerHost">) {
  if (typeof window === "undefined") return;
  queue.push({
    ...event,
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
