"use client";

import { useTranslations } from "next-intl";
import { useHydrated } from "@/lib/use-hydrated";
import { useSelection } from "@/store/selection";

/** Na liście kupiec widzi, które modele już są w zestawieniu (ta sama kropka co przy kolorach na karcie produktu). */
export function InSelectionMark({ productId }: { productId: string }) {
  const t = useTranslations("catalog");
  const hydrated = useHydrated();
  const count = useSelection((s) => s.lines.filter((l) => l.productId === productId).length);
  if (!hydrated || count === 0) return null;
  return (
    <span className="tabular ml-auto inline-flex items-center gap-1.5 text-foreground">
      <span aria-hidden className="size-1.5 rounded-full bg-foreground" />
      {t("inSelection", { count })}
    </span>
  );
}
