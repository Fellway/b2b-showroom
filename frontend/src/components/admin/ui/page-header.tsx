import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import type { ReactNode } from "react";

/** Nagłówek strony panelu: wysokość paska (64 px), tytuł Body 500, akcje po prawej, opcjonalny powrót. */
export function PageHeader({
  title,
  meta,
  actions,
  back,
}: {
  title: ReactNode;
  meta?: ReactNode;
  actions?: ReactNode;
  back?: { href: string; label: string };
}) {
  return (
    <header className="sticky top-0 z-20 border-b border-line bg-background">
      <div className="flex min-h-14 flex-wrap items-center gap-x-6 gap-y-2 px-5 py-3 md:min-h-(--header-height) md:px-8">
        <div className="min-w-0 flex-1">
          {back && (
            <Link href={back.href} className="mb-0.5 inline-flex items-center gap-1.5 text-meta text-ink-3 hover:text-foreground">
              <ArrowLeft aria-hidden className="size-3.5" strokeWidth={1.5} />
              {back.label}
            </Link>
          )}
          <div className="flex flex-wrap items-baseline gap-x-3">
            <h1 className="truncate text-body font-medium">{title}</h1>
            {meta && <div className="text-meta text-ink-3">{meta}</div>}
          </div>
        </div>
        {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
      </div>
    </header>
  );
}

/** Treść strony panelu z marginesem i maksymalną szerokością czytelną dla tabel i formularzy. */
export function PageBody({ children, narrow }: { children: ReactNode; narrow?: boolean }) {
  return <div className={narrow ? "max-w-3xl px-5 py-6 md:px-8 md:py-8" : "max-w-[1400px] px-5 py-6 md:px-8 md:py-8"}>{children}</div>;
}
