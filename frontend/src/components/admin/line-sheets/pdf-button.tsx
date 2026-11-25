"use client";

import { Download } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { errorMessage } from "@/lib/admin/errors";
import { downloadPdf } from "@/lib/admin/line-sheets";
import { cn } from "@/lib/utils";

/**
 * Pobranie PDF zestawienia z tokenem panelu (link bez nagłówka Authorization nie zadziała — D23).
 * `compact` — sama ikona w wierszu tabeli; domyślnie przycisk główny w nagłówku strony.
 */
export function PdfButton({ id, referenceNumber, compact }: { id: string; referenceNumber: string; compact?: boolean }) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function download() {
    setError(null);
    setPending(true);
    try {
      await downloadPdf(id, referenceNumber);
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setPending(false);
    }
  }

  if (compact) {
    return (
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation();
          void download();
        }}
        disabled={pending}
        aria-label={error ? `${error} Pobierz PDF ${referenceNumber}` : `Pobierz PDF ${referenceNumber}`}
        title={error ?? "Pobierz PDF"}
        className={cn(
          "inline-flex size-8 items-center justify-center hover:text-foreground disabled:cursor-wait",
          error ? "text-danger" : "text-ink-3",
        )}
      >
        <Download aria-hidden className="size-4" strokeWidth={1.5} />
      </button>
    );
  }
  return (
    <div className="flex items-center gap-3">
      {error && (
        <span role="alert" className="text-meta text-danger">
          {error}
        </span>
      )}
      <Button onClick={download} disabled={pending}>
        <Download aria-hidden strokeWidth={1.5} />
        {pending ? "Pobieranie…" : "Pobierz PDF"}
      </Button>
    </div>
  );
}
