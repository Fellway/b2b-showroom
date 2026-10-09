"use client";

import { Eye, EyeOff } from "lucide-react";
import { useState } from "react";
import { useFlash } from "@/components/admin/ui/flash";
import { errorMessage } from "@/lib/admin/errors";
import { useChangeStatus, type ProductListItem } from "@/lib/admin/product-list";

/** Szybkie ukrycie / publikacja z listy. Archiwum wymaga świadomej decyzji w edytorze, więc tu go nie ma. */
export function StatusAction({ product }: { product: ProductListItem }) {
  const flash = useFlash();
  const change = useChangeStatus();
  const [error, setError] = useState<string | null>(null);

  if (product.status === "ARCHIVED") return null;
  const hide = product.status === "PUBLISHED";

  const run = () => {
    setError(null);
    change.mutate(
      { id: product.id, transition: hide ? "HIDE" : "PUBLISH", version: product.version },
      {
        onSuccess: () => flash(hide ? `Ukryto: ${product.name}` : `Opublikowano: ${product.name}`),
        onError: (e) => setError(errorMessage(e)),
      },
    );
  };

  return (
    <div className="flex flex-col items-end gap-1">
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation();
          run();
        }}
        disabled={change.isPending}
        className="inline-flex h-8 items-center gap-1.5 px-2 text-ink-2 hover:text-foreground disabled:text-ink-3"
      >
        {hide ? <EyeOff aria-hidden className="size-4" strokeWidth={1.5} /> : <Eye aria-hidden className="size-4" strokeWidth={1.5} />}
        {change.isPending ? "Zapisywanie…" : hide ? "Ukryj" : "Publikuj"}
        <span className="sr-only">: {product.name}</span>
      </button>
      {error && (
        <p role="alert" className="max-w-60 text-right text-meta text-danger">
          {error}
        </p>
      )}
    </div>
  );
}
