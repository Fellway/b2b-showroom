"use client";

import { ArrowLeft, ArrowRight } from "lucide-react";

export function ListPagination({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (page: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <nav aria-label="Strony listy" className="mt-6 flex items-center justify-between border-t border-line pt-4">
      <button
        type="button"
        onClick={() => onPage(page - 1)}
        disabled={page === 0}
        className="inline-flex items-center gap-1.5 px-1 py-2 text-ink-2 hover:text-foreground disabled:invisible"
      >
        <ArrowLeft aria-hidden className="size-4" strokeWidth={1.5} />
        Poprzednia
      </button>
      <span className="tabular text-meta text-ink-3">
        Strona {page + 1} z {totalPages}
      </span>
      <button
        type="button"
        onClick={() => onPage(page + 1)}
        disabled={page >= totalPages - 1}
        className="inline-flex items-center gap-1.5 px-1 py-2 text-ink-2 hover:text-foreground disabled:invisible"
      >
        Następna
        <ArrowRight aria-hidden className="size-4" strokeWidth={1.5} />
      </button>
    </nav>
  );
}
