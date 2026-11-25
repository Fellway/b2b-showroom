"use client";

import { useLocale, useTranslations } from "next-intl";
import * as Flags from "country-flag-icons/react/3x2";
import type { CountryCode } from "libphonenumber-js/min";
import { Check, ChevronDown, Search } from "lucide-react";
import { useId, useMemo, useState, type ReactNode } from "react";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { phoneCountries, type PhoneCountry } from "@/lib/phone";
import { cn } from "@/lib/utils";

type FlagComponent = (props: { className?: string; "aria-hidden"?: boolean }) => React.JSX.Element;

/** Flaga jako SVG (Windows nie wyświetla emoji flag). Brak flagi dla kodu = neutralny prostokąt. */
export function CountryFlag({ code, className }: { code: string; className?: string }) {
  const Flag = (Flags as unknown as Record<string, FlagComponent | undefined>)[code];
  const box = cn("inline-block h-3.5 w-[21px] shrink-0 border border-black/10", className);
  return Flag ? <Flag aria-hidden className={box} /> : <span aria-hidden className={cn(box, "bg-well")} />;
}

/**
 * Telefon = kraj (numer kierunkowy, z flagą) + numer krajowy. Kraj wybiera się z listy z wyszukiwarką
 * (nazwa, kod kraju albo numer kierunkowy) — opiekun handlowy zawsze wie, dokąd dzwoni.
 */
export function PhoneField({
  label,
  country,
  onCountryChange,
  national,
  onNationalChange,
  hint,
  error,
  required,
}: {
  label: ReactNode;
  country: CountryCode;
  onCountryChange: (country: CountryCode) => void;
  national: string;
  onNationalChange: (value: string) => void;
  hint?: ReactNode;
  error?: string | null;
  required?: boolean;
}) {
  const t = useTranslations("account.phone");
  const locale = useLocale();
  const id = useId();
  const listId = `${id}-countries`;
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const countries = useMemo(() => phoneCountries(locale), [locale]);
  const selected = countries.find((c) => c.code === country) ?? countries[0];

  const normalized = query.trim().toLocaleLowerCase(locale).replace(/^\+/, "");
  const matches = normalized
    ? countries.filter(
        (c) =>
          c.name.toLocaleLowerCase(locale).includes(normalized) ||
          c.code.toLowerCase() === normalized ||
          c.dialCode.slice(1).startsWith(normalized),
      )
    : countries;

  function choose(next: PhoneCountry) {
    onCountryChange(next.code);
    setOpen(false);
    setQuery("");
    // po wyborze kraju kursor wraca do numeru
    requestAnimationFrame(() => document.getElementById(id)?.focus());
  }

  return (
    <div className="grid content-start gap-1.5">
      <label htmlFor={id} className="text-meta text-ink-3">
        {label}
      </label>
      <div className="flex">
        <Popover
          open={open}
          onOpenChange={(next) => {
            setOpen(next);
            if (!next) setQuery("");
          }}
        >
          <PopoverTrigger
            aria-label={t("country", { country: selected.name, code: selected.dialCode })}
            aria-haspopup="listbox"
            className={cn(
              "flex h-11 shrink-0 items-center gap-2 border border-r-0 border-edge px-3 text-small outline-none transition-colors",
              "hover:bg-well focus-visible:border-foreground data-[state=open]:bg-well",
              error && "border-danger",
            )}
          >
            <CountryFlag code={selected.code} />
            <span className="tabular">{selected.dialCode}</span>
            <ChevronDown aria-hidden className="size-3.5 text-ink-3" strokeWidth={1.5} />
          </PopoverTrigger>
          <PopoverContent align="start" sideOffset={6} className="w-80 gap-0 p-0">
            <div className="flex items-center gap-2 border-b border-line px-3">
              <Search aria-hidden className="size-4 shrink-0 text-ink-3" strokeWidth={1.5} />
              <input
                autoFocus
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && matches[0]) {
                    e.preventDefault();
                    choose(matches[0]);
                  }
                }}
                placeholder={t("search")}
                aria-label={t("search")}
                aria-controls={listId}
                className="h-10 w-full bg-transparent text-small outline-none placeholder:text-ink-3"
              />
            </div>
            <ul id={listId} role="listbox" aria-label={t("list")} className="max-h-72 overflow-y-auto py-1">
              {matches.length === 0 && <li className="px-3 py-2 text-ink-3">{t("noResults")}</li>}
              {matches.map((option) => {
                const active = option.code === selected.code;
                return (
                  <li key={option.code} role="option" aria-selected={active}>
                    <button
                      type="button"
                      onClick={() => choose(option)}
                      className="flex w-full items-center gap-3 px-3 py-2 text-left hover:bg-well focus-visible:bg-well focus-visible:outline-none"
                    >
                      <CountryFlag code={option.code} />
                      <span className="min-w-0 flex-1 truncate">{option.name}</span>
                      <span className="tabular text-ink-3">{option.dialCode}</span>
                      {active ? (
                        <Check aria-hidden className="size-4 shrink-0" strokeWidth={1.5} />
                      ) : (
                        <span aria-hidden className="size-4 shrink-0" />
                      )}
                    </button>
                  </li>
                );
              })}
            </ul>
          </PopoverContent>
        </Popover>
        <input
          id={id}
          type="tel"
          inputMode="tel"
          autoComplete="tel-national"
          required={required}
          maxLength={24}
          value={national}
          onChange={(e) => onNationalChange(e.target.value)}
          aria-invalid={error ? true : undefined}
          aria-describedby={error || hint ? `${id}-note` : undefined}
          className="h-11 min-w-0 flex-1 border border-edge bg-transparent px-3 text-small outline-none transition-colors focus-visible:border-foreground aria-invalid:border-danger"
        />
      </div>
      {error ? (
        <p id={`${id}-note`} role="alert" className="text-meta text-danger">
          {error}
        </p>
      ) : hint ? (
        <p id={`${id}-note`} className="text-meta text-ink-3">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
