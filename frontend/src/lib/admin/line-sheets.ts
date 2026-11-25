"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { adminApi, query, type Page } from "./api";

export type LineSheetSummary = {
  id: string;
  publicId: string;
  referenceNumber: string;
  language: string;
  companyName: string | null;
  contactName: string | null;
  email: string | null;
  phone: string | null;
  itemCount: number;
  createdAt: string;
  /** Konto kupca (D23); `null` dla zestawień sprzed kont i po usunięciu konta. */
  buyerAccountId: string | null;
};

export type LineSheetItem = {
  position: number;
  productId: string | null;
  variantId: string | null;
  styleCode: string;
  productName: string;
  colorName: string;
  sizeCode: string;
  sku: string;
  ean: string;
  wholesalePrice: number;
  retailPrice: number;
  imageStorageKey: string | null;
  quantity: number | null;
};

export type LineSheetDetail = {
  id: string;
  publicId: string;
  referenceNumber: string;
  language: string;
  currency: string;
  recipient: { companyName: string | null; contactName: string | null; email: string | null; phone: string | null; taxId: string | null };
  message: string | null;
  privacyAcceptedAt: string | null;
  salesRepresentative: { id: string; fullName: string; email: string; phone: string | null } | null;
  pdfAvailable: boolean;
  pdfGeneratedAt: string | null;
  createdAt: string;
  items: LineSheetItem[];
  wholesaleTotal: number | null;
  buyerAccountId: string | null;
  version: number;
};

export const LINE_SHEETS_PAGE_SIZE = 25;

export const useLineSheets = (page: number) =>
  useQuery({
    queryKey: ["admin", "line-sheets", page],
    queryFn: () => adminApi.get<Page<LineSheetSummary>>(`/line-sheets${query({ page, size: LINE_SHEETS_PAGE_SIZE })}`),
    placeholderData: keepPreviousData,
  });

export const useLineSheet = (id: string) =>
  useQuery({ queryKey: ["admin", "line-sheets", "detail", id], queryFn: () => adminApi.get<LineSheetDetail>(`/line-sheets/${id}`) });

/** PDF zawiera ceny i dane kupca — pobierany z tokenem panelu (D23), ten sam plik, który dostał kupiec. */
export const downloadPdf = (id: string, referenceNumber: string) =>
  adminApi.download(`/line-sheets/${id}/pdf`, `${referenceNumber}.pdf`);

/** Miniatury pozycji to zdjęcia produktów z publicznego magazynu mediów. */
export const mediaUrl = (storageKey: string | null) => (storageKey ? `/api/v1/media/${storageKey}` : null);

const TIME_ZONE = "Europe/Warsaw";

export const formatDateTime = (iso: string | null | undefined) =>
  iso
    ? new Intl.DateTimeFormat("pl-PL", { dateStyle: "medium", timeStyle: "short", timeZone: TIME_ZONE }).format(new Date(iso))
    : "—";

export const formatDate = (iso: string) =>
  new Intl.DateTimeFormat("pl-PL", { dateStyle: "medium", timeZone: TIME_ZONE }).format(new Date(iso));

export const formatMoney = (value: number, currency: string) =>
  new Intl.NumberFormat("pl-PL", { style: "currency", currency }).format(value);

export const LANGUAGE_NAME: Record<string, string> = { PL: "polski", EN: "angielski", CS: "czeski", UK: "ukraiński", pl: "polski", en: "angielski", cs: "czeski", uk: "ukraiński" };
