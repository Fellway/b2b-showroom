"use client";

import type { FormEvent, ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetDescription, SheetTitle } from "@/components/ui/sheet";

/**
 * Panel boczny z formularzem (DESIGN.md › Panel boczny): nagłówek 64 px, przewijana treść, stopka z akcjami
 * odcięta linią. Edycja rekordów (słowniki, opiekunowie, konta) bez opuszczania listy, która zostaje pod zaciemnieniem.
 */
export function FormSheet({
  open,
  onOpenChange,
  title,
  description,
  onSubmit,
  footer,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  /** Widoczny opis pod nagłówkiem; bez niego tytuł służy też jako opis dla czytników ekranu. */
  description?: string;
  onSubmit: (event: FormEvent) => void;
  footer: ReactNode;
  children: ReactNode;
}) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="right"
        closeLabel="Zamknij"
        className="w-full gap-0 border-line p-0 shadow-panel data-[side=right]:w-full data-[side=right]:sm:max-w-[520px]"
      >
        <form onSubmit={onSubmit} className="flex h-full min-h-0 flex-col">
          <div className="flex h-(--header-height) shrink-0 items-center border-b border-line px-6 pr-14">
            <SheetTitle className="truncate text-body font-medium">{title}</SheetTitle>
          </div>
          <SheetDescription className={description ? "px-6 pt-5 text-small text-ink-2" : "sr-only"}>
            {description ?? title}
          </SheetDescription>
          <div className="min-h-0 flex-1 overflow-y-auto px-6 py-5">
            <div className="flex flex-col gap-5">{children}</div>
          </div>
          <div className="flex shrink-0 flex-wrap items-center gap-2 border-t border-line px-6 py-4">{footer}</div>
        </form>
      </SheetContent>
    </Sheet>
  );
}

/** Standardowa stopka: błąd zapisu nad akcjami, dodatkowa akcja po lewej (np. Usuń), Anuluj i akcja główna po prawej. */
export function SheetActions({
  onCancel,
  submitLabel,
  pending,
  error,
  start,
}: {
  onCancel: () => void;
  submitLabel: string;
  pending?: boolean;
  error?: string | null;
  start?: ReactNode;
}) {
  return (
    <div className="flex w-full flex-col gap-3">
      {error && (
        <p role="alert" className="text-small text-danger">
          {error}
        </p>
      )}
      <div className="flex items-center gap-2">
        {start}
        <Button type="button" variant="text" className="ml-auto" onClick={onCancel}>
          Anuluj
        </Button>
        <Button type="submit" variant="primary" size="lg" disabled={pending}>
          {pending ? "Zapisywanie…" : submitLabel}
        </Button>
      </div>
    </div>
  );
}

/** Grupa pól w panelu (np. jeden język tłumaczenia) — linia nad, tytuł Small 500, bez tła. */
export function FieldGroup({ title, aside, children }: { title: string; aside?: ReactNode; children: ReactNode }) {
  return (
    <fieldset className="flex flex-col gap-4 border-t border-line pt-4">
      <div className="flex items-baseline justify-between gap-3">
        <legend className="float-left text-small font-medium">{title}</legend>
        {aside}
      </div>
      {children}
    </fieldset>
  );
}
