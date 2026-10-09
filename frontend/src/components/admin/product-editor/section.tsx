import type { ReactNode } from "react";

/** Krok edytora: kotwica dla nawigacji bocznej, tytuł Body 500 i opis pod nim; kroki rozdziela linia. */
export function Section({
  id,
  title,
  description,
  actions,
  children,
}: {
  id: string;
  title: string;
  description?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section id={id} aria-labelledby={`${id}-title`} className="scroll-mt-[calc(var(--header-height)+1.5rem)] border-t border-line py-10 first:border-t-0 first:pt-2">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 id={`${id}-title`} className="text-body font-medium">
            {title}
          </h2>
          {description && <p className="mt-1 max-w-[70ch] text-ink-3">{description}</p>}
        </div>
        {actions && <div className="flex items-center gap-2">{actions}</div>}
      </div>
      {children}
    </section>
  );
}
