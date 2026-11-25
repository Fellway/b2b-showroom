"use client";

import { useId, type ComponentProps, type ReactNode } from "react";
import { cn } from "@/lib/utils";

const control =
  "w-full border border-edge bg-transparent px-3 text-small outline-none transition-colors focus-visible:border-foreground focus-visible:outline-none aria-invalid:border-danger";

/** Pole formularza strony publicznej: etykieta Meta nad kontrolką, podpowiedź albo błąd pod nią. */
export function TextField({
  label,
  hint,
  error,
  className,
  ...props
}: ComponentProps<"input"> & { label: ReactNode; hint?: ReactNode; error?: string | null }) {
  const id = useId();
  return (
    <div className={cn("grid content-start gap-1.5", className)}>
      <label htmlFor={id} className="text-meta text-ink-3">
        {label}
      </label>
      <input
        id={id}
        {...props}
        aria-invalid={error ? true : undefined}
        aria-describedby={error || hint ? `${id}-note` : undefined}
        className={cn(control, "h-11")}
      />
      <Note id={`${id}-note`} hint={hint} error={error} />
    </div>
  );
}

export function SelectField({
  label,
  error,
  children,
  className,
  ...props
}: ComponentProps<"select"> & { label: ReactNode; error?: string | null }) {
  const id = useId();
  return (
    <div className={cn("grid content-start gap-1.5", className)}>
      <label htmlFor={id} className="text-meta text-ink-3">
        {label}
      </label>
      <select
        id={id}
        {...props}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-note` : undefined}
        className={cn(control, "h-11 appearance-none bg-[length:16px] bg-[right_0.75rem_center] bg-no-repeat pr-9")}
        style={{
          backgroundImage:
            "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23767676' stroke-width='1.5'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E\")",
        }}
      >
        {children}
      </select>
      <Note id={`${id}-note`} error={error} />
    </div>
  );
}

export function TextAreaField({ label, className, ...props }: ComponentProps<"textarea"> & { label: ReactNode }) {
  const id = useId();
  return (
    <div className={cn("grid content-start gap-1.5", className)}>
      <label htmlFor={id} className="text-meta text-ink-3">
        {label}
      </label>
      <textarea id={id} rows={3} {...props} className={cn(control, "resize-y py-2")} />
    </div>
  );
}

export function CheckField({
  label,
  invalid,
  ...props
}: ComponentProps<"input"> & { label: ReactNode; invalid?: boolean }) {
  return (
    <label className="flex cursor-pointer items-start gap-3 text-meta text-ink-2">
      <input type="checkbox" {...props} aria-invalid={invalid || undefined} className="mt-0.5 size-4 shrink-0 accent-foreground" />
      <span>{label}</span>
    </label>
  );
}

function Note({ id, hint, error }: { id: string; hint?: ReactNode; error?: string | null }) {
  if (error) {
    return (
      <p id={id} role="alert" className="text-meta text-danger">
        {error}
      </p>
    );
  }
  return hint ? (
    <p id={id} className="text-meta text-ink-3">
      {hint}
    </p>
  ) : null;
}

/** Komunikat formularza: błąd (role=alert) albo potwierdzenie (aria-live). */
export function FormMessage({ tone, children }: { tone: "error" | "info"; children: ReactNode }) {
  return tone === "error" ? (
    <p role="alert" className="text-small text-danger">
      {children}
    </p>
  ) : (
    <p aria-live="polite" className="bg-well px-4 py-3 text-small text-ink-2">
      {children}
    </p>
  );
}

export function SubmitButton({ pending, children, className, ...props }: ComponentProps<"button"> & { pending?: boolean }) {
  return (
    <button
      type="submit"
      disabled={pending}
      {...props}
      className={cn(
        "flex h-12 w-full items-center justify-center bg-foreground px-6 text-small font-medium text-background transition-colors hover:bg-ink-2 disabled:cursor-wait disabled:bg-ink-2",
        className,
      )}
    >
      {children}
    </button>
  );
}

/** Wąska kolumna stron konta (logowanie, rejestracja, potwierdzenia) — rytm jak w szufladzie zestawienia. */
export function AccountColumn({ title, lead, children, wide }: { title: ReactNode; lead?: ReactNode; children: ReactNode; wide?: boolean }) {
  return (
    <div className={cn("mx-auto w-full px-5 pb-20 pt-10 md:pt-14", wide ? "max-w-[640px]" : "max-w-[440px]")}>
      <h1 className="text-title font-medium text-balance md:text-[1.75rem] md:leading-[2.25rem]">{title}</h1>
      {lead && <p className="mt-2 text-ink-2">{lead}</p>}
      <div className="mt-8">{children}</div>
    </div>
  );
}

/** Bezpieczny cel przekierowania po logowaniu: tylko ścieżka w obrębie aplikacji (bez `//`, bez schematu). */
export function safeNext(next: string | null, fallback = "/account"): string {
  return next && next.startsWith("/") && !next.startsWith("//") && !next.startsWith("/\\") ? next : fallback;
}
