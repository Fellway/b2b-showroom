"use client";

import { useEffect, useState } from "react";

/**
 * Token z linku w e-mailu (`?token=`) odczytany raz i usunięty z paska adresu — nie zostaje w historii przeglądarki
 * ani w nagłówku Referer. Przy odświeżeniu strony tokenu już nie ma (link jest jednorazowy).
 *
 * Adres czyścimy dopiero w efekcie: przy hydratacji router Next.js zapisuje do historii swój adres, więc wcześniejsza
 * zmiana zostałaby nadpisana. `history.replaceState` po hydratacji jest zsynchronizowany z routerem.
 */
export function useTokenFromLink(): string | null {
  const [token] = useState<string | null>(() =>
    typeof window === "undefined" ? null : new URL(window.location.href).searchParams.get("token"),
  );
  useEffect(() => {
    const url = new URL(window.location.href);
    if (!url.searchParams.has("token")) return;
    url.searchParams.delete("token");
    window.history.replaceState(window.history.state, "", url.pathname + url.search + url.hash);
  }, []);
  return token;
}

/** Jednorazowe akcje (tokeny z e-maili) — ten sam token wysyłamy najwyżej raz, także w trybie StrictMode. */
const inFlight = new Map<string, Promise<Response>>();

export function sendOnce(key: string, send: () => Promise<Response>): Promise<Response> {
  let pending = inFlight.get(key);
  if (!pending) {
    pending = send();
    inFlight.set(key, pending);
  }
  return pending.then((response) => response.clone());
}
