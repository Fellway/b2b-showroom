import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function EmptyState({ title, children, action }: { title: string; children?: ReactNode; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-start gap-3 border-t border-line py-16">
      <p className="font-medium">{title}</p>
      {children && <div className="max-w-[56ch] text-ink-2">{children}</div>}
      {action}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div role="alert" className="flex flex-col items-start gap-3 border-t border-line py-16">
      <p className="text-danger">{message}</p>
      {onRetry && (
        <button type="button" onClick={onRetry} className="underline underline-offset-4 hover:no-underline">
          Spróbuj ponownie
        </button>
      )}
    </div>
  );
}

/** Zastępcze wiersze podczas ładowania — ten sam rytm co tabela, bez migających spinnerów. */
export function LoadingRows({ rows = 6, className }: { rows?: number; className?: string }) {
  return (
    <div aria-busy="true" aria-label="Ładowanie" className={cn("flex flex-col", className)}>
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="flex items-center gap-4 border-t border-line py-3">
          <div className="h-12 w-8 bg-well" />
          <div className="h-3 w-48 bg-well" />
          <div className="ml-auto h-3 w-20 bg-well" />
        </div>
      ))}
    </div>
  );
}
