"use client";

import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";

/**
 * Potwierdzenie operacji nieodwracalnej (usunięcie). Jedyne miejsce panelu, gdzie przerwanie pracy
 * modalem jest uzasadnione.
 */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  children,
  confirmLabel,
  onConfirm,
  pending,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  children: ReactNode;
  confirmLabel: string;
  onConfirm: () => void;
  pending?: boolean;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md gap-0 rounded-none border-line p-6 shadow-menu">
        <DialogTitle className="text-body font-medium">{title}</DialogTitle>
        <DialogDescription asChild>
          <div className="mt-2 text-small text-ink-2">{children}</div>
        </DialogDescription>
        <div className="mt-6 flex justify-end gap-2">
          <Button variant="text" onClick={() => onOpenChange(false)}>
            Anuluj
          </Button>
          <Button variant="primary" onClick={onConfirm} disabled={pending} className="bg-danger hover:bg-danger/85">
            {pending ? "Usuwanie…" : confirmLabel}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
