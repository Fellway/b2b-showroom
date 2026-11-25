"use client";

import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";
import type { Price } from "@/lib/types";

/** Maksymalna liczba pozycji zestawienia — zgodna z limitem backendu (LineSheet.MAX_ITEMS). */
export const MAX_LINES = 200;

/**
 * Jedna pozycja zestawienia = jeden wariant (model × kolor × rozmiar). Dane do wyświetlenia są migawką z chwili
 * dodania; PDF i tak składa backend z aktualnego katalogu w języku żądania.
 *
 * Wybór działa także dla gościa (lookbook), dlatego pozycja nie przechowuje cen ani EAN — te zna tylko zalogowany
 * kupiec (D23) i są dociągane z API przy wyświetlaniu podsumowania.
 */
export type SelectionLine = {
  variantId: string;
  productId: string;
  slug: string;
  styleCode: string;
  name: string;
  colorId: string;
  colorName: string;
  colorHex: string | null;
  size: string;
  sizeOrder: number;
  imageUrl: string | null;
  quantity: number | null;
};

type SelectionState = {
  lines: SelectionLine[];
  drawerOpen: boolean;
  /** Zastępuje wszystkie rozmiary danego modelu w danym kolorze (wybór na karcie produktu). */
  replaceColor: (productId: string, colorId: string, lines: SelectionLine[]) => void;
  setQuantity: (variantId: string, quantity: number | null) => void;
  remove: (variantId: string) => void;
  removeGroup: (productId: string, colorId: string) => void;
  clear: () => void;
  setDrawerOpen: (open: boolean) => void;
};

export const useSelection = create<SelectionState>()(
  persist(
    (set) => ({
      lines: [],
      drawerOpen: false,
      replaceColor: (productId, colorId, incoming) =>
        set((state) => {
          const others = state.lines.filter((l) => !(l.productId === productId && l.colorId === colorId));
          return { lines: [...others, ...incoming].slice(0, MAX_LINES) };
        }),
      setQuantity: (variantId, quantity) =>
        set((state) => ({
          lines: state.lines.map((l) => (l.variantId === variantId ? { ...l, quantity } : l)),
        })),
      remove: (variantId) => set((state) => ({ lines: state.lines.filter((l) => l.variantId !== variantId) })),
      removeGroup: (productId, colorId) =>
        set((state) => ({
          lines: state.lines.filter((l) => !(l.productId === productId && l.colorId === colorId)),
        })),
      clear: () => set({ lines: [] }),
      setDrawerOpen: (drawerOpen) => set({ drawerOpen }),
    }),
    {
      name: "fs-line-sheet",
      version: 2,
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ lines: state.lines }),
      // v1 zapisywała w przeglądarce ceny i EAN — od D23 tych danych nie trzymamy lokalnie
      migrate: (persisted) => {
        const stored = (persisted as { lines?: SelectionLine[] } | undefined)?.lines ?? [];
        const lines: SelectionLine[] = stored.map((line) => ({
          variantId: line.variantId,
          productId: line.productId,
          slug: line.slug,
          styleCode: line.styleCode,
          name: line.name,
          colorId: line.colorId,
          colorName: line.colorName,
          colorHex: line.colorHex,
          size: line.size,
          sizeOrder: line.sizeOrder,
          imageUrl: line.imageUrl,
          quantity: line.quantity,
        }));
        return { lines };
      },
    },
  ),
);

export type SelectionGroup = {
  key: string;
  productId: string;
  colorId: string;
  slug: string;
  styleCode: string;
  name: string;
  colorName: string;
  colorHex: string | null;
  imageUrl: string | null;
  lines: SelectionLine[];
};

/** Pozycje pogrupowane po modelu i kolorze, w kolejności dodania; rozmiary w kolejności rozmiarówki. */
export function groupLines(lines: SelectionLine[]): SelectionGroup[] {
  const groups = new Map<string, SelectionGroup>();
  for (const line of lines) {
    const key = `${line.productId}:${line.colorId}`;
    const group = groups.get(key) ?? {
      key,
      productId: line.productId,
      colorId: line.colorId,
      slug: line.slug,
      styleCode: line.styleCode,
      name: line.name,
      colorName: line.colorName,
      colorHex: line.colorHex,
      imageUrl: line.imageUrl,
      lines: [],
    };
    group.lines.push(line);
    groups.set(key, group);
  }
  return [...groups.values()].map((g) => ({ ...g, lines: g.lines.sort((a, b) => a.sizeOrder - b.sizeOrder) }));
}

/**
 * Podsumowanie wyboru. Wartość hurtowa jest liczona tylko, gdy znamy ceny (zalogowany kupiec) — `prices` po `productId`.
 */
export function summarize(lines: SelectionLine[], prices?: Map<string, Price>) {
  const withQuantity = lines.filter((l) => l.quantity != null);
  const priced = prices ? withQuantity.filter((l) => prices.has(l.productId)) : [];
  return {
    lines: lines.length,
    units: withQuantity.reduce((sum, l) => sum + (l.quantity ?? 0), 0),
    value: priced.reduce((sum, l) => sum + (l.quantity ?? 0) * (prices?.get(l.productId)?.wholesale ?? 0), 0),
    hasValue: priced.length > 0,
    partial: withQuantity.length > 0 && withQuantity.length < lines.length,
    hasQuantities: withQuantity.length > 0,
    currency: (priced[0] && prices?.get(priced[0].productId)?.currency) ?? "PLN",
  };
}
