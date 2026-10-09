"use client";

import { Check } from "lucide-react";
import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from "react";

type Flash = { id: number; message: string };

const FlashContext = createContext<(message: string) => void>(() => undefined);

/** Krótkie potwierdzenie po zapisie („Zapisano”) — czarny pasek w lewym dolnym rogu, ogłaszany czytnikom ekranu. */
export function FlashProvider({ children }: { children: ReactNode }) {
  const [flash, setFlash] = useState<Flash | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const show = useCallback((message: string) => {
    if (timer.current) clearTimeout(timer.current);
    setFlash({ id: Date.now(), message });
    timer.current = setTimeout(() => setFlash(null), 2600);
  }, []);
  return (
    <FlashContext.Provider value={show}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed bottom-6 left-6 z-50">
        {flash && (
          <p
            key={flash.id}
            className="flex items-center gap-2 bg-foreground px-4 py-3 text-small text-background shadow-menu animate-in fade-in-0 slide-in-from-bottom-2 duration-300"
          >
            <Check aria-hidden className="size-4" strokeWidth={2} />
            {flash.message}
          </p>
        )}
      </div>
    </FlashContext.Provider>
  );
}

export const useFlash = () => useContext(FlashContext);
