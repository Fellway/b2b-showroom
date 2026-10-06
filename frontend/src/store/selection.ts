"use client";

import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

/** Maksymalna liczba pozycji zestawienia — zgodna z limitem backendu (LineSheet.MAX_ITEMS). */
export const MAX_LINES = 200;

/**
 * Jedna pozycja zestawienia = jeden wariant (model × kolor × rozmiar). Dane do wyświetlenia są migawką z chwili
 * dodania; PDF i tak składa backend z aktualnego katalogu w języku żądania.
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
  ean: string;
  imageUrl: string | null;
  wholesale: number;
  retail: number;
  currency: string;
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
      version: 1,
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ lines: state.lines }),
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

export function summarize(lines: SelectionLine[]) {
  const withQuantity = lines.filter((l) => l.quantity != null);
  return {
    lines: lines.length,
    units: withQuantity.reduce((sum, l) => sum + (l.quantity ?? 0), 0),
    value: withQuantity.reduce((sum, l) => sum + (l.quantity ?? 0) * l.wholesale, 0),
    partial: withQuantity.length > 0 && withQuantity.length < lines.length,
    hasQuantities: withQuantity.length > 0,
    currency: lines[0]?.currency ?? "PLN",
  };
}
