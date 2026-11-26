"use client";

import { useTranslations } from "next-intl";
import { ClipboardList } from "lucide-react";
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
      // na telefonie ikona zamiast słowa — nagłówek mieści logo, język, konto i licznik bez łamania
      className={cn(
        "flex h-11 items-center gap-2 px-2 text-small transition-colors sm:h-10 sm:px-3",
        visibleCount > 0 ? "text-foreground" : "text-ink-2 hover:text-foreground",
      )}
    >
      <ClipboardList aria-hidden className="size-[18px] sm:hidden" strokeWidth={1.5} />
      <span className="hidden sm:inline">{t("selection")}</span>
      <span className="sr-only sm:hidden">{t("selection")}</span>
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
