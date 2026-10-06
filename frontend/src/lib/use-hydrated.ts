"use client";

import { useSyncExternalStore } from "react";

const subscribe = () => () => {};

/** `true` dopiero po hydratacji — dane z localStorage nie mogą trafić do HTML renderowanego na serwerze. */
export function useHydrated() {
  return useSyncExternalStore(subscribe, () => true, () => false);
}
