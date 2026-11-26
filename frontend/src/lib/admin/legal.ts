"use client";

import { useQuery } from "@tanstack/react-query";
import type { LegalDocumentType, LegalVersionSummary } from "@/lib/types";
import { adminApi, ApiError } from "./api";
import { errorMessage } from "./errors";

/** Dokumenty prawne w panelu (D25) — `/api/v1/admin/legal`. */

export type LegalLanguage = "pl" | "en" | "cs" | "uk";
export const LEGAL_LANGUAGES: { code: LegalLanguage; label: string }[] = [
  { code: "pl", label: "Polski" },
  { code: "en", label: "Angielski" },
  { code: "cs", label: "Czeski" },
  { code: "uk", label: "Ukraiński" },
];

export type LegalText = { title: string; body: string; changeSummary: string | null };

export type LegalOverview = {
  type: LegalDocumentType;
  current: LegalVersionSummary | null;
  draft: LegalVersionSummary | null;
  published: LegalVersionSummary[];
};

/** Klucze `texts` z API to nazwy języków („PL”, „EN”…) — normalizujemy do kodów. */
export type LegalVersion = LegalVersionSummary & {
  texts: Partial<Record<LegalLanguage, LegalText>>;
  missingLanguages: string[];
  version: number;
};

export const LEGAL_TYPE_LABEL: Record<LegalDocumentType, string> = {
  TERMS: "Regulamin",
  PRIVACY_POLICY: "Polityka prywatności",
};

export const LEGAL_SLUG: Record<LegalDocumentType, "terms" | "privacy"> = { TERMS: "terms", PRIVACY_POLICY: "privacy" };

export const legalKey = ["admin", "legal"] as const;

export const useLegalOverview = () =>
  useQuery({ queryKey: [...legalKey, "overview"], queryFn: () => adminApi.get<LegalOverview[]>("/legal") });

function normalize(version: LegalVersion): LegalVersion {
  const texts: LegalVersion["texts"] = {};
  Object.entries(version.texts ?? {}).forEach(([key, text]) => {
    texts[key.toLowerCase() as LegalLanguage] = text;
  });
  return { ...version, texts };
}

export const useLegalVersion = (id: string | null) =>
  useQuery({
    queryKey: [...legalKey, "version", id],
    queryFn: async () => normalize(await adminApi.get<LegalVersion>(`/legal/versions/${id}`)),
    enabled: id !== null,
  });

export const createDraft = async (type: LegalDocumentType) =>
  normalize(await adminApi.post<LegalVersion>(`/legal/${LEGAL_SLUG[type]}/drafts`, {}));

export const saveDraft = async (id: string, version: number, texts: LegalVersion["texts"]) =>
  normalize(await adminApi.put<LegalVersion>(`/legal/versions/${id}`, { version, texts }));

export const publishDraft = (id: string, version: number, requireAcceptance: boolean) =>
  adminApi.post<LegalVersion>(`/legal/versions/${id}/publish`, { version, requireAcceptance });

export const deleteDraft = (id: string) => adminApi.delete(`/legal/versions/${id}`);

const MESSAGES: Record<string, string> = {
  "legal.document.translation-missing": "Uzupełnij tytuł i treść we wszystkich językach — dopiero wtedy można opublikować wersję.",
  "legal.document.draft-exists": "Szkic tego dokumentu już istnieje — odśwież stronę.",
  "legal.document.published-immutable": "Opublikowanej wersji nie można zmienić — przygotuj nową wersję.",
  "legal.document.not-found": "Ta wersja dokumentu już nie istnieje.",
  "legal.text.title": "Tytuł jest wymagany (do 200 znaków) w każdym uzupełnionym języku.",
  "legal.text.body": "Treść jest wymagana (do 100 000 znaków) w każdym uzupełnionym języku.",
  "legal.text.change-summary": "Opis zmian może mieć do 1000 znaków.",
  "data.concurrent-modification": "Ktoś inny zmienił ten szkic — odśwież stronę, aby zobaczyć aktualną treść.",
};

export function legalError(error: unknown) {
  if (error instanceof ApiError && error.status === 403) return "Publikować nowe wersje dokumentów może tylko administrator.";
  return error instanceof ApiError && error.code && MESSAGES[error.code] ? MESSAGES[error.code] : errorMessage(error);
}
