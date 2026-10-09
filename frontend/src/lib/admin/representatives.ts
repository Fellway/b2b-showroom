"use client";

import { useQuery } from "@tanstack/react-query";
import { adminApi } from "./api";
import { errorMessage } from "./errors";
import { ApiError } from "./api";

export type Representative = {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  active: boolean;
  sortOrder: number;
  version: number;
};

export type RepresentativeInput = Omit<Representative, "id" | "version"> & { version?: number };

export const representativesKey = ["admin", "representatives"] as const;

export const useRepresentatives = () =>
  useQuery({ queryKey: representativesKey, queryFn: () => adminApi.get<Representative[]>("/sales-representatives") });

const MESSAGES: Record<string, string> = {
  "contact.representative.email": "Podaj poprawny adres e-mail.",
  "contact.representative.email-taken": "Opiekun z tym adresem e-mail już istnieje.",
  "contact.representative.full-name": "Podaj imię i nazwisko (do 120 znaków).",
  "contact.representative.phone": "Podaj poprawny numer telefonu (do 32 znaków).",
  "contact.representative.sort-order": "Kolejność nie może być ujemna.",
  "contact.representative.not-found": "Opiekun został już usunięty.",
};

export function representativeError(error: unknown) {
  return error instanceof ApiError && error.code && MESSAGES[error.code] ? MESSAGES[error.code] : errorMessage(error);
}
