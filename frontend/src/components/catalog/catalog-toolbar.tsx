"use client";

import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { Check, ChevronDown, Search, X } from "lucide-react";
import { useState, useTransition, type FormEvent, type ReactNode } from "react";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { usePathname, useRouter } from "@/i18n/navigation";
import type { CatalogSort, Facets } from "@/lib/types";
import { cn } from "@/lib/utils";

type Props = {
  facets: Facets;
  selectedColors: string[];
  selectedSizes: string[];
  query: string;
  sort: CatalogSort;
  resultsLabel: string;
};

const SORTS: CatalogSort[] = ["RECOMMENDED", "NEWEST", "PRICE_ASC", "PRICE_DESC"];

export function CatalogToolbar({ facets, selectedColors, selectedSizes, query, sort, resultsLabel }: Props) {
  const t = useTranslations("catalog");
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [pending, startTransition] = useTransition();
  const [search, setSearch] = useState(query);

  /** Każda zmiana filtra wraca na pierwszą stronę wyników; stan żyje w URL (link można przesłać dalej). */
  function update(mutate: (params: URLSearchParams) => void) {
    const params = new URLSearchParams(searchParams);
    mutate(params);
    params.delete("page");
    const queryString = params.toString();
    startTransition(() => {
      router.replace(queryString ? `${pathname}?${queryString}` : pathname, { scroll: false });
    });
  }

  const toggle = (key: "color" | "size", id: string) =>
    update((params) => {
      const values = params.getAll(key);
      params.delete(key);
      const next = values.includes(id) ? values.filter((v) => v !== id) : [...values, id];
      next.forEach((v) => params.append(key, v));
    });

  const submitSearch = (event: FormEvent) => {
    event.preventDefault();
    update((params) => (search.trim() ? params.set("q", search.trim()) : params.delete("q")));
  };

  const clearAll = () =>
    update((params) => {
      ["color", "size", "q"].forEach((k) => params.delete(k));
      setSearch("");
    });

  const activeChips = [
    ...facets.colors.filter((c) => selectedColors.includes(c.id)).map((c) => ({ key: "color" as const, id: c.id, label: c.name })),
    ...facets.sizes.filter((s) => selectedSizes.includes(s.id)).map((s) => ({ key: "size" as const, id: s.id, label: s.code })),
  ];

  return (
    <div className={cn("border-y border-line transition-opacity", pending && "opacity-60")} aria-busy={pending}>
      <div className="flex flex-wrap items-center gap-x-1 gap-y-2 py-2">
        <FilterPopover label={t("color")} count={selectedColors.length}>
          <ul className="flex max-h-80 flex-col overflow-y-auto py-1">
            {facets.colors.map((color) => {
              const checked = selectedColors.includes(color.id);
              return (
                <li key={color.id}>
                  <button
                    type="button"
                    role="menuitemcheckbox"
                    aria-checked={checked}
                    onClick={() => toggle("color", color.id)}
                    className="flex w-full items-center gap-3 px-3 py-2 text-left text-small hover:bg-well"
                  >
                    <span
                      aria-hidden
                      className="size-4 shrink-0 border border-black/15"
                      style={{ backgroundColor: color.hex ?? "transparent" }}
                    />
                    <span className="flex-1">{color.name}</span>
                    <span className="tabular text-meta text-ink-3">{color.productCount}</span>
                    <Check aria-hidden className={cn("size-4", checked ? "opacity-100" : "opacity-0")} strokeWidth={1.5} />
                  </button>
                </li>
              );
            })}
          </ul>
        </FilterPopover>

        <FilterPopover label={t("size")} count={selectedSizes.length}>
          <div className="grid grid-cols-4 gap-1.5 p-3">
            {facets.sizes.map((size) => {
              const checked = selectedSizes.includes(size.id);
              return (
                <button
                  key={size.id}
                  type="button"
                  aria-pressed={checked}
                  onClick={() => toggle("size", size.id)}
                  className={cn(
                    "tabular flex h-10 items-center justify-center border text-small transition-colors",
                    checked ? "border-foreground bg-foreground text-background" : "border-line hover:border-foreground",
                  )}
                >
                  {size.code}
                </button>
              );
            })}
          </div>
        </FilterPopover>

        <form onSubmit={submitSearch} role="search" className="relative ml-1 flex min-w-0 flex-1 basis-48 items-center md:max-w-72">
          <Search aria-hidden className="pointer-events-none absolute left-0 size-4 text-ink-3" strokeWidth={1.5} />
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            maxLength={100}
            placeholder={t("search")}
            aria-label={t("search")}
            className="h-10 w-full bg-transparent pl-6 text-small outline-none placeholder:text-ink-3 focus-visible:outline-none"
          />
          <button type="submit" className="sr-only">
            {t("searchSubmit")}
          </button>
        </form>

        <div className="ml-auto flex items-center gap-2">
          <p className="tabular text-small text-ink-3" aria-live="polite">
            {resultsLabel}
          </p>
          <FilterPopover label={`${t("sort")}: ${t(`sortOptions.${sort}`)}`} align="end">
            <ul className="py-1" role="menu">
              {SORTS.map((option) => (
                <li key={option}>
                  <button
                    type="button"
                    role="menuitemradio"
                    aria-checked={option === sort}
                    onClick={() =>
                      update((params) => (option === "RECOMMENDED" ? params.delete("sort") : params.set("sort", option)))
                    }
                    className="flex w-full items-center justify-between gap-6 px-3 py-2 text-left text-small hover:bg-well"
                  >
                    {t(`sortOptions.${option}`)}
                    <Check aria-hidden className={cn("size-4", option === sort ? "opacity-100" : "opacity-0")} strokeWidth={1.5} />
                  </button>
                </li>
              ))}
            </ul>
          </FilterPopover>
        </div>
      </div>

      {(activeChips.length > 0 || query) && (
        <div className="flex flex-wrap items-center gap-2 border-t border-line py-3">
          {activeChips.map((chip) => (
            <button
              key={`${chip.key}-${chip.id}`}
              type="button"
              onClick={() => toggle(chip.key, chip.id)}
              className="inline-flex h-8 items-center gap-1.5 border border-line px-2.5 text-meta hover:border-foreground"
            >
              {chip.label}
              <X aria-hidden className="size-3.5" strokeWidth={1.5} />
            </button>
          ))}
          {query && (
            <span className="inline-flex h-8 items-center border border-line px-2.5 text-meta">„{query}”</span>
          )}
          <button type="button" onClick={clearAll} className="ml-1 text-meta underline underline-offset-4 hover:no-underline">
            {t("clearFilters")}
          </button>
        </div>
      )}
    </div>
  );
}

function FilterPopover({
  label,
  count = 0,
  align = "start",
  children,
}: {
  label: string;
  count?: number;
  align?: "start" | "end";
  children: ReactNode;
}) {
  return (
    <Popover>
      <PopoverTrigger className="flex h-10 items-center gap-1.5 px-3 text-small first:-ml-3 hover:text-foreground data-[state=open]:underline data-[state=open]:underline-offset-4">
        {label}
        {count > 0 && (
          <span className="tabular inline-flex h-5 min-w-5 items-center justify-center bg-foreground px-1 text-meta text-background">
            {count}
          </span>
        )}
        <ChevronDown aria-hidden className="size-3.5 text-ink-3" strokeWidth={1.5} />
      </PopoverTrigger>
      <PopoverContent
        align={align}
        sideOffset={6}
        className="w-72 rounded-none border-line p-0 shadow-[0_12px_32px_-16px_rgb(17_17_17/0.3)]"
      >
        {children}
      </PopoverContent>
    </Popover>
  );
}
