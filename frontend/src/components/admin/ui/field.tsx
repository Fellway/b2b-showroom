"use client";

import { ChevronDown } from "lucide-react";
import { useId, type ComponentProps, type ReactElement, type ReactNode, cloneElement, isValidElement } from "react";
import { cn } from "@/lib/utils";

const control =
  "w-full border border-edge bg-background px-3 text-small text-foreground outline-none transition-colors placeholder:text-ink-3 hover:border-foreground focus-visible:border-foreground focus-visible:outline-none disabled:border-line disabled:bg-well disabled:text-ink-3 aria-invalid:border-danger";

/**
 * Etykieta (Meta, ink-3) nad kontrolką, opcjonalna podpowiedź i komunikat błędu pod nią.
 * Łączy etykietę i opisy z kontrolką przez id / aria-describedby.
 */
export function Field({
  label,
  hint,
  error,
  children,
  className,
  aside,
}: {
  label: ReactNode;
  hint?: ReactNode;
  error?: string | null;
  children: ReactElement<{ id?: string; "aria-invalid"?: boolean; "aria-describedby"?: string }>;
  className?: string;
  aside?: ReactNode;
}) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(" ") || undefined;
  const child = isValidElement(children)
    ? cloneElement(children, { id, "aria-invalid": Boolean(error) || undefined, "aria-describedby": describedBy })
    : children;
  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <div className="flex items-baseline justify-between gap-3">
        <label htmlFor={id} className="text-meta text-ink-3">
          {label}
        </label>
        {aside}
      </div>
      {child}
      {hint && !error && (
        <p id={hintId} className="text-meta text-ink-3">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} role="alert" className="text-meta text-danger">
          {error}
        </p>
      )}
    </div>
  );
}

export function TextInput({ className, ...props }: ComponentProps<"input">) {
  return <input {...props} className={cn(control, "h-10", className)} />;
}

export function NumberInput({ className, ...props }: ComponentProps<"input">) {
  return <input inputMode="decimal" {...props} className={cn(control, "tabular h-10 text-right", className)} />;
}

export function TextArea({ className, rows = 4, ...props }: ComponentProps<"textarea">) {
  return <textarea rows={rows} {...props} className={cn(control, "resize-y py-2 leading-relaxed", className)} />;
}

export function Select({ className, children, ...props }: ComponentProps<"select">) {
  return (
    <div className="relative">
      <select {...props} className={cn(control, "h-10 appearance-none pr-9", className)}>
        {children}
      </select>
      <ChevronDown aria-hidden className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-ink-3" strokeWidth={1.5} />
    </div>
  );
}

export function Checkbox({ label, className, ...props }: ComponentProps<"input"> & { label: ReactNode }) {
  return (
    <label className={cn("inline-flex cursor-pointer items-center gap-2.5 text-small", props.disabled && "cursor-not-allowed text-ink-3", className)}>
      <input type="checkbox" {...props} className="size-4 shrink-0 accent-foreground" />
      {label}
    </label>
  );
}
