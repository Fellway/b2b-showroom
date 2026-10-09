"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { adminApi, query } from "./api";

export type RankingEntry = {
  productId: string;
  known: boolean;
  slug: string | null;
  styleCode: string | null;
  name: string | null;
  status: string | null;
  count: number;
};

export type AnalyticsEventType =
  | "PAGE_VIEW"
  | "PRODUCT_VIEW"
  | "SELECTION_ADDED"
  | "SELECTION_REMOVED"
  | "LINE_SHEET_GENERATED"
  | "CONTACT_CLICKED";

export type AnalyticsSummary = {
  from: string;
  to: string;
  eventTotals: Partial<Record<AnalyticsEventType, number>>;
  uniqueVisitors: number;
  lineSheetsCreated: number;
  topViewedProducts: RankingEntry[];
  topSelectedProducts: RankingEntry[];
  topLineSheetProducts: RankingEntry[];
};

/** Zakres jako daty YYYY-MM-DD; backend traktuje `to` włącznie (do końca dnia UTC). */
export const useAnalyticsSummary = (from: string, to: string) =>
  useQuery({
    queryKey: ["admin", "analytics", from, to],
    queryFn: () => adminApi.get<AnalyticsSummary>(`/analytics/summary${query({ from, to, lang: "pl" })}`),
    placeholderData: keepPreviousData,
  });
