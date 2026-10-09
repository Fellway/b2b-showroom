"use client";

import { useQuery } from "@tanstack/react-query";
import { adminApi, ApiError } from "./api";
import { errorMessage } from "./errors";
import type { AdminRole } from "./session";

export type AdminUser = {
  id: string;
  email: string;
  displayName: string;
  role: AdminRole;
  enabled: boolean;
  lockedUntil: string | null;
  lastLoginAt: string | null;
  version: number;
};

export const ROLE_LABEL: Record<AdminRole, string> = { ADMIN: "Administrator", EDITOR: "Redaktor" };
export const ROLE_HINT: Record<AdminRole, string> = {
  ADMIN: "Pełny dostęp, łącznie z zarządzaniem kontami.",
  EDITOR: "Katalog, słowniki, zestawienia i statystyki — bez kont.",
};

export const PASSWORD_MIN = 12;
export const PASSWORD_MAX = 128;

export const usersKey = ["admin", "users"] as const;

export const useAdminUsers = (enabled: boolean) =>
  useQuery({ queryKey: usersKey, queryFn: () => adminApi.get<AdminUser[]>("/users"), enabled });

const MESSAGES: Record<string, string> = {
  "identity.cannot-demote-self": "Nie możesz odebrać sobie roli administratora.",
  "identity.cannot-disable-self": "Nie możesz wyłączyć własnego konta.",
  "identity.last-admin": "To ostatni aktywny administrator — najpierw nadaj tę rolę komuś innemu.",
  "identity.email-taken": "Konto z tym adresem e-mail już istnieje.",
  "identity.email": "Podaj poprawny adres e-mail.",
  "identity.display-name": "Podaj nazwę wyświetlaną (do 120 znaków).",
  "identity.password.policy": `Hasło musi mieć od ${PASSWORD_MIN} do ${PASSWORD_MAX} znaków.`,
  "identity.admin.not-found": "Konto zostało już usunięte.",
  "identity.admin.concurrent-modification": "Ktoś inny zmienił to konto w międzyczasie. Dane zostały odświeżone.",
};

export function userError(error: unknown) {
  return error instanceof ApiError && error.code && MESSAGES[error.code] ? MESSAGES[error.code] : errorMessage(error);
}
