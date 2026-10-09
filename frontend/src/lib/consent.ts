"use client";

import { useSyncExternalStore } from "react";

/**
 * Zgoda na analitykę (ePrivacy art. 5 ust. 3 / RODO). Jedyna kategoria wymagająca zgody — zestawienie
 * w localStorage to funkcja, o którą kupiec sam prosi, a zapamiętanie samej decyzji jest niezbędne.
 * Decyzja ważna 12 miesięcy, potem pytamy ponownie; zmiana wersji tekstu też wymusza ponowne pytanie.
 */
const KEY = "fs-consent";
const VERSION = 1;
const MAX_AGE_MS = 365 * 24 * 60 * 60 * 1000;

export type ConsentState = "unknown" | "granted" | "denied";

type Stored = { analytics: boolean; decidedAt: string; version: number };

let settingsOpen = false;
/** Element, który otworzył ustawienia — po decyzji fokus wraca tam, a nie na <body>. */
let returnFocusTo: HTMLElement | null = null;

function restoreFocus() {
  const target = returnFocusTo;
  returnFocusTo = null;
  if (target?.isConnected) requestAnimationFrame(() => target.focus());
}
const listeners = new Set<() => void>();
const notify = () => listeners.forEach((listener) => listener());

function read(): ConsentState {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return "unknown";
    const stored = JSON.parse(raw) as Stored;
    if (stored.version !== VERSION || Date.now() - Date.parse(stored.decidedAt) > MAX_AGE_MS) {
      // decyzja wygasła — identyfikator nie może zostać na urządzeniu bez aktualnej zgody
      forgetIdentifiers();
      return "unknown";
    }
    return stored.analytics ? "granted" : "denied";
  } catch {
    return "unknown";
  }
}

export const consent = {
  get: read,
  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
  decide(analytics: boolean) {
    try {
      localStorage.setItem(KEY, JSON.stringify({ analytics, decidedAt: new Date().toISOString(), version: VERSION } satisfies Stored));
    } catch {
      // przeglądarka bez dostępu do pamięci — decyzja obowiązuje do końca wizyty
    }
    if (!analytics) forgetIdentifiers();
    settingsOpen = false;
    notify();
    restoreFocus();
  },
  openSettings(trigger?: HTMLElement | null) {
    returnFocusTo = trigger ?? null;
    settingsOpen = true;
    notify();
  },
  closeSettings() {
    settingsOpen = false;
    notify();
    restoreFocus();
  },
  isSettingsOpen: () => settingsOpen,
};

/** Wycofanie zgody usuwa identyfikatory analityczne zapisane wcześniej w przeglądarce. */
export function forgetIdentifiers() {
  try {
    localStorage.removeItem("fs-visitor");
    sessionStorage.removeItem("fs-session");
  } catch {
    // brak dostępu do pamięci — nie ma czego usuwać
  }
}

export function useConsent(): { state: ConsentState; settingsOpen: boolean } {
  const state = useSyncExternalStore(consent.subscribe, read, () => "granted" as ConsentState);
  const open = useSyncExternalStore(consent.subscribe, () => settingsOpen, () => false);
  return { state, settingsOpen: open };
}
