"use client";

import { useSyncExternalStore } from "react";
import { session, type SessionState } from "./session";

const LOADING: SessionState = { status: "loading" };

export function useSession(): SessionState {
  return useSyncExternalStore(session.subscribe, session.snapshot, () => LOADING);
}
