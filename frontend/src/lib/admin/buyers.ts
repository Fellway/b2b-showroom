"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import type { BuyerConsent } from "@/lib/types";
import { adminApi, query, type Page } from "./api";

/** Konta kupców (D23) — `GET /api/v1/admin/buyers`. */
export type BuyerStatus = "PENDING_EMAIL" | "PENDING_APPROVAL" | "ACTIVE" | "REJECTED" | "SUSPENDED";

export type BuyerSummary = {
  id: string;
  email: string;
  companyName: string;
  countryCode: string;
  taxId: string;
  contactName: string;
  status: BuyerStatus;
  createdAt: string;
};

export type BuyerDetail = BuyerSummary & {
  phone: string;
  language: string;
  emailVerifiedAt: string | null;
  reviewedAt: string | null;
  reviewedBy: string | null;
  reviewNote: string | null;
  lastLoginAt: string | null;
  lockedUntil: string | null;
  /** Rejestr zgód (D25), od najnowszej. */
  consents: BuyerConsent[];
  version: number;
};

export const BUYER_STATUS_LABEL: Record<BuyerStatus, string> = {
  PENDING_EMAIL: "Niepotwierdzony e-mail",
  PENDING_APPROVAL: "Do akceptacji",
  ACTIVE: "Aktywny",
  REJECTED: "Odrzucony",
  SUSPENDED: "Zawieszony",
};

/** Zakładki listy: domyślnie wnioski do decyzji. */
export const BUYER_FILTERS: { value: BuyerStatus | "ALL"; label: string }[] = [
  { value: "PENDING_APPROVAL", label: "Do akceptacji" },
  { value: "ACTIVE", label: "Aktywni" },
  { value: "PENDING_EMAIL", label: "Niepotwierdzony e-mail" },
  { value: "REJECTED", label: "Odrzuceni" },
  { value: "SUSPENDED", label: "Zawieszeni" },
  { value: "ALL", label: "Wszyscy" },
];

export const BUYERS_PAGE_SIZE = 25;

export const buyersKey = ["admin", "buyers"] as const;

export const useBuyers = (status: BuyerStatus | "ALL", search: string, page: number) =>
  useQuery({
    queryKey: [...buyersKey, "list", status, search, page],
    queryFn: () =>
      adminApi.get<Page<BuyerSummary>>(
        `/buyers${query({ status: status === "ALL" ? undefined : status, q: search, page, size: BUYERS_PAGE_SIZE })}`,
      ),
    placeholderData: keepPreviousData,
  });

export const useBuyer = (id: string | null) =>
  useQuery({
    queryKey: [...buyersKey, "detail", id],
    queryFn: () => adminApi.get<BuyerDetail>(`/buyers/${id}`),
    enabled: id !== null,
  });

/** Licznik wniosków w nawigacji panelu — odświeżany co minutę. */
export const usePendingBuyersCount = () =>
  useQuery({
    queryKey: [...buyersKey, "pending-count"],
    queryFn: () => adminApi.get<{ count: number }>("/buyers/pending-count"),
    refetchInterval: 60_000,
    select: (result) => result.count,
  });

export type BuyerDecision = "approve" | "reject" | "suspend" | "reactivate";

export const decide = (id: string, decision: BuyerDecision, version: number, note?: string) =>
  adminApi.post(`/buyers/${id}/${decision}`, { version, note: note?.trim() ? note.trim() : null });

export const deleteBuyer = (id: string) => adminApi.delete(`/buyers/${id}`);
