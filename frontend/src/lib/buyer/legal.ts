"use client";

import { useSyncExternalStore } from "react";
import type { BuyerLegalStatus } from "@/lib/types";
import { buyerApi, invalidatePricing, TERMS_REQUIRED_EVENT } from "./api";
import { buyerSession } from "./session";

/**
 * Dokumenty prawne zalogowanego kupca (D25): co musi zaakceptować (nowy regulamin blokuje ceny i zestawienia — także
 * w API), o czym go informujemy (nowa polityka prywatności) i historia zgód. Stan pobierany raz po zalogowaniu
 * i ponownie, gdy API odpowie kodem `identity.buyer.terms-acceptance-required` (nowa wersja w trakcie sesji).
 */

export type BuyerLegalState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "ready"; data: BuyerLegalStatus }
  | { status: "error" };

let state: BuyerLegalState = { status: "idle" };
let inFlight: Promise<void> | null = null;
const listeners = new Set<() => void>();

function setState(next: BuyerLegalState) {
  state = next;
  listeners.forEach((listener) => listener());
}

export const buyerLegal = {
  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
  snapshot: () => state,

  load(): Promise<void> {
    inFlight ??= buyerApi
      .get<BuyerLegalStatus>("/me/legal")
      .then((data) => setState({ status: "ready", data }))
      .catch(() => setState({ status: "error" }))
      .finally(() => {
        inFlight = null;
      });
    if (state.status !== "ready") setState({ status: "loading" });
    return inFlight;
  },

  /** Akceptacja / przyjęcie do wiadomości obowiązujących wersji; rzuca BuyerApiError (np. nowsza wersja — 409). */
  async accept(versionIds: string[], lang: string) {
    const data = await buyerApi.post<BuyerLegalStatus>(`/me/legal/accept?lang=${encodeURIComponent(lang)}`, { versionIds });
    setState({ status: "ready", data });
    // ceny odrzucone wcześniej przez bramę regulaminu — pobieramy od nowa
    invalidatePricing();
  },
};

buyerSession.subscribe(() => {
  const session = buyerSession.snapshot();
  if (session.status === "authenticated") {
    if (state.status === "idle") void buyerLegal.load();
  } else if (state.status !== "idle") {
    setState({ status: "idle" });
  }
});

if (typeof window !== "undefined") {
  // nowa wersja regulaminu opublikowana w trakcie sesji — API zaczęło odmawiać cen i zestawień
  window.addEventListener(TERMS_REQUIRED_EVENT, () => void buyerLegal.load());
}

const IDLE: BuyerLegalState = { status: "idle" };

export function useBuyerLegal(): BuyerLegalState {
  return useSyncExternalStore(buyerLegal.subscribe, buyerLegal.snapshot, () => IDLE);
}
