"use client";

import { Check, Pencil, X } from "lucide-react";
import { useState, type FormEvent, type KeyboardEvent } from "react";
import { useFlash } from "@/components/admin/ui/flash";
import { errorMessage } from "@/lib/admin/errors";
import { formatPln, parseMoney, useUpdateWholesale, type ProductListItem } from "@/lib/admin/product-list";

/** Cena hurtowa z edycją w miejscu (Enter zapisuje, Esc anuluje) — zmiana ceny bez opuszczania listy. */
export function PriceCell({ product }: { product: ProductListItem }) {
  const flash = useFlash();
  const update = useUpdateWholesale();
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState("");
  const [error, setError] = useState<string | null>(null);

  const start = () => {
    setValue(product.price.wholesale.toFixed(2).replace(".", ","));
    setError(null);
    setEditing(true);
  };

  const cancel = () => {
    setEditing(false);
    setError(null);
  };

  const submit = (event?: FormEvent) => {
    event?.preventDefault();
    const wholesale = parseMoney(value);
    if (wholesale === null) {
      setError("Podaj kwotę większą od zera, np. 159,00.");
      return;
    }
    if (wholesale === product.price.wholesale) {
      cancel();
      return;
    }
    update.mutate(
      { id: product.id, wholesale },
      {
        onSuccess: () => {
          setEditing(false);
          flash(`Cena hurtowa ${product.name}: ${formatPln(wholesale, product.price.currency)}`);
        },
        onError: (e) => setError(errorMessage(e)),
      },
    );
  };

  const onKeyDown = (event: KeyboardEvent) => {
    if (event.key === "Escape") {
      event.stopPropagation();
      cancel();
    }
  };

  if (!editing) {
    return (
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation();
          start();
        }}
        aria-label={`Zmień cenę hurtową: ${product.name}`}
        className="group/price -mr-1 inline-flex items-center gap-1.5 px-1 py-1 tabular hover:bg-background"
      >
        {formatPln(product.price.wholesale, product.price.currency)}
        <Pencil aria-hidden className="size-3.5 text-ink-3 opacity-0 transition-opacity group-hover/price:opacity-100 group-focus-visible/price:opacity-100" strokeWidth={1.5} />
      </button>
    );
  }

  return (
    <form onSubmit={submit} onClick={(e) => e.stopPropagation()} className="flex flex-col items-end gap-1">
      <div className="flex items-center">
        <input
          autoFocus
          inputMode="decimal"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={onKeyDown}
          aria-label={`Nowa cena hurtowa netto: ${product.name}`}
          aria-invalid={Boolean(error) || undefined}
          disabled={update.isPending}
          className="tabular h-8 w-24 border border-foreground bg-background px-2 text-right text-small outline-none aria-invalid:border-danger"
        />
        <button type="submit" disabled={update.isPending} aria-label="Zapisz cenę" className="flex size-8 items-center justify-center bg-foreground text-background hover:bg-ink-2 disabled:bg-ink-2">
          <Check aria-hidden className="size-4" strokeWidth={1.5} />
        </button>
        <button type="button" onClick={cancel} aria-label="Anuluj" className="flex size-8 items-center justify-center text-ink-3 hover:text-foreground">
          <X aria-hidden className="size-4" strokeWidth={1.5} />
        </button>
      </div>
      {error && (
        <p role="alert" className="max-w-56 text-right text-meta text-danger">
          {error}
        </p>
      )}
    </form>
  );
}
