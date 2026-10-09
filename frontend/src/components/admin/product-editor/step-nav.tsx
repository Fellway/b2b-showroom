"use client";

import { useEffect, useState } from "react";
import { cn } from "@/lib/utils";

export type Step = { id: string; label: string; note?: string; attention?: boolean };

/**
 * Kroki edytora z podświetlaniem bieżącej sekcji (scroll-spy). Uwaga (`attention`) to pusta kropka
 * przy kroku z brakami — bez koloru, zgodnie z DESIGN.md.
 */
export function StepNav({ steps }: { steps: Step[] }) {
  const [current, setCurrent] = useState(steps[0]?.id);

  const ids = steps.map((s) => s.id).join("|");
  useEffect(() => {
    const stepIds = ids.split("|");
    // bieżący krok = ostatnia sekcja, której początek minął przyklejony nagłówek (z zapasem na czytelność)
    const update = () => {
      const threshold = 160;
      let active = stepIds[0];
      for (const id of stepIds) {
        const element = document.getElementById(id);
        if (element && element.getBoundingClientRect().top <= threshold) active = id;
      }
      // na samym dole strony ostatnia krótka sekcja może nie dojść do progu
      if (innerHeight + scrollY >= document.documentElement.scrollHeight - 4) active = stepIds[stepIds.length - 1];
      setCurrent(active);
    };
    const frame = requestAnimationFrame(update);
    addEventListener("scroll", update, { passive: true });
    addEventListener("resize", update);
    return () => {
      cancelAnimationFrame(frame);
      removeEventListener("scroll", update);
      removeEventListener("resize", update);
    };
  }, [ids]);

  return (
    <nav aria-label="Kroki edycji" className="sticky top-[calc(var(--header-height)+2rem)] hidden self-start lg:block">
      <ol className="flex flex-col border-l border-line">
        {steps.map((step) => {
          const active = step.id === current;
          return (
            <li key={step.id}>
              <a
                href={`#${step.id}`}
                aria-current={active ? "step" : undefined}
                className={cn(
                  "-ml-px flex flex-col border-l py-2 pl-4 transition-colors",
                  active ? "border-foreground text-foreground" : "border-transparent text-ink-2 hover:text-foreground",
                )}
              >
                <span className="flex items-center gap-2">
                  {step.label}
                  {step.attention && <span aria-hidden className="size-1.5 rounded-full border border-foreground" />}
                </span>
                {step.note && <span className="text-meta text-ink-3">{step.note}</span>}
              </a>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
