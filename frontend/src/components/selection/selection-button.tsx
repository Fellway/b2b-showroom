"use client";

import { useTranslations } from "next-intl";
import { useSelection } from "@/store/selection";
import { useHydrated } from "@/lib/use-hydrated";
import { cn } from "@/lib/utils";

export function SelectionButton() {
  const t = useTranslations("site");
  const hydrated = useHydrated();
  const count = useSelection((s) => s.lines.length);
  const setDrawerOpen = useSelection((s) => s.setDrawerOpen);
  const visibleCount = hydrated ? count : 0;

  return (
    <button
      type="button"
      onClick={() => setDrawerOpen(true)}
      aria-haspopup="dialog"
      className={cn(
        "flex h-10 items-center gap-2 px-3 text-small transition-colors",
        visibleCount > 0 ? "text-foreground" : "text-ink-2 hover:text-foreground",
      )}
    >
      {t("selection")}
      <span
        key={visibleCount}
        className={cn(
          "tabular inline-flex h-5 min-w-5 items-center justify-center px-1.5 text-meta",
          visibleCount > 0 ? "bg-foreground text-background animate-in zoom-in-75 duration-300" : "border border-line",
        )}
      >
        {visibleCount}
      </span>
    </button>
  );
}
