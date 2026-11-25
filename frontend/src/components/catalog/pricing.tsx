"use client";

import { useLocale, useTranslations } from "next-intl";
import { createContext, useContext, type ReactNode } from "react";
import { Link, usePathname } from "@/i18n/navigation";
import { usePricing, useBuyerSession, type PricingState } from "@/lib/buyer/api";
import { formatMarkup, formatMoney } from "@/lib/format";
import { cn } from "@/lib/utils";

const PricingContext = createContext<PricingState>({ status: "loading" });

/** Ceny wszystkich kart na stronie katalogu — jedno żądanie do API kupca zamiast osobnego na każdą kartę. */
export function PricingScope({ productIds, children }: { productIds: string[]; children: ReactNode }) {
  const state = usePricing(productIds);
  return <PricingContext.Provider value={state}>{children}</PricingContext.Provider>;
}

/** Linia cen na karcie produktu: ceny dla kupca, informacja dla gościa (karta jest linkiem — bez zagnieżdżonych linków). */
export function CardPrice({ productId }: { productId: string }) {
  const t = useTranslations("catalog");
  const locale = useLocale();
  const state = useContext(PricingContext);

  if (state.status === "anonymous") {
    return <p className="mt-0.5 text-small text-ink-3">{t("pricesLocked")}</p>;
  }
  if (state.status === "loading") {
    return <p aria-hidden className="mt-1.5 h-3.5 w-40 bg-well" />;
  }
  const pricing = state.status === "ready" ? state.byProduct.get(productId) : undefined;
  if (!pricing) return null;
  const { price } = pricing;
  return (
    <p className="tabular mt-0.5 flex flex-wrap items-baseline gap-x-3 gap-y-0.5 text-small">
      <span>
        <span className="text-ink-3">{t("wholesale")} </span>
        <span className="font-medium">{formatMoney(price.wholesale, price.currency, locale)}</span>
      </span>
      <span className="text-ink-2">
        <span className="text-ink-3">{t("retail")} </span>
        {formatMoney(price.retail, price.currency, locale)}
      </span>
      <span className="text-ink-3" title={t("markup")}>
        {formatMarkup(price.markup, locale)}
      </span>
    </p>
  );
}

/** Pasek nad listą dla gościa: po co zakładać konto. Zalogowany kupiec go nie widzi. */
export function PriceGate({ namespace = "catalog", className }: { namespace?: "catalog" | "product"; className?: string }) {
  const t = useTranslations(`${namespace}.gate`);
  const state = useBuyerSession();
  const pathname = usePathname();
  if (state.status !== "anonymous") return null;
  return (
    <div className={cn("flex flex-col gap-3 bg-well px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:gap-6", className)}>
      <div className="min-w-0">
        <p className="text-small font-medium">{t("title")}</p>
        <p className="mt-0.5 max-w-[72ch] text-small text-ink-2">{t("lead")}</p>
      </div>
      <div className="flex shrink-0 items-center gap-4 text-small">
        <Link
          href={{ pathname: "/account/login", query: { next: pathname } }}
          className="flex h-10 items-center bg-foreground px-4 font-medium text-background transition-colors hover:bg-ink-2"
        >
          {t("signIn")}
        </Link>
        <Link href={{ pathname: "/account/register", query: { next: pathname } }} className="underline underline-offset-4 hover:no-underline">
          {t("register")}
        </Link>
      </div>
    </div>
  );
}
