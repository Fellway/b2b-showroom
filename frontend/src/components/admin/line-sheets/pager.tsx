"use client";

import { ArrowLeft, ArrowRight } from "lucide-react";

/** Paginacja tekstowa wg DESIGN.md › Navigation: „poprzednia / strona n z m / następna” nad linią. */
export function Pager({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  if (totalPages <= 1) return null;
  const button = "inline-flex items-center gap-1.5 px-3 py-2 underline-offset-4 hover:underline disabled:invisible";
  return (
    <nav aria-label="Strony listy" className="mt-6 flex items-center justify-between border-t border-line pt-4">
      <button type="button" className={`${button} -ml-3`} disabled={page === 0} onClick={() => onChange(page - 1)}>
        <ArrowLeft aria-hidden className="size-4" strokeWidth={1.5} />
        Poprzednia
      </button>
      <span className="tabular text-ink-3">
        Strona {page + 1} z {totalPages}
      </span>
      <button type="button" className={`${button} -mr-3`} disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Następna
        <ArrowRight aria-hidden className="size-4" strokeWidth={1.5} />
      </button>
    </nav>
  );
}
