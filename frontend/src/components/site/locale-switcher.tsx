"use client";

import { useParams, useSearchParams } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { ChevronDown, Check } from "lucide-react";
import { useState } from "react";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { usePathname, useRouter } from "@/i18n/navigation";
import { routing, type Locale } from "@/i18n/routing";

export function LocaleSwitcher() {
  const t = useTranslations();
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();
  const params = useParams();
  const searchParams = useSearchParams();
  const [open, setOpen] = useState(false);

  function change(next: Locale) {
    setOpen(false);
    const query = Object.fromEntries(searchParams.entries());
    // @ts-expect-error -- ścieżka i parametry pochodzą z bieżącej trasy, więc są zgodne z jej wzorcem
    router.replace({ pathname, params, query }, { locale: next, scroll: false });
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        aria-label={`${t("site.language")}: ${t(`languages.${locale}`)}`}
        className="flex h-10 items-center gap-1 px-3 text-small uppercase text-ink-2 transition-colors hover:text-foreground data-[state=open]:text-foreground"
      >
        {locale}
        <ChevronDown aria-hidden className="size-3.5" strokeWidth={1.5} />
      </PopoverTrigger>
      <PopoverContent align="end" sideOffset={8} className="w-48 p-1">
        <ul role="listbox" aria-label={t("site.language")}>
          {routing.locales.map((code) => (
            <li key={code}>
              <button
                type="button"
                role="option"
                aria-selected={code === locale}
                lang={code}
                onClick={() => change(code)}
                className="flex w-full items-center justify-between px-3 py-2 text-left text-small hover:bg-well"
              >
                {t(`languages.${code}`)}
                {code === locale && <Check aria-hidden className="size-4" strokeWidth={1.5} />}
              </button>
            </li>
          ))}
        </ul>
      </PopoverContent>
    </Popover>
  );
}
