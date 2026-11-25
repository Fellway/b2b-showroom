"use client";

import { useTranslations } from "next-intl";
import { CircleUserRound } from "lucide-react";
import { Link } from "@/i18n/navigation";
import { useBuyerSession } from "@/lib/buyer/api";

/** W nagłówku: „Zaloguj się” dla gościa, „Konto” dla zalogowanego kupca. */
export function AccountLink() {
  const t = useTranslations("site");
  const state = useBuyerSession();
  if (state.status === "loading") {
    return <span aria-hidden className="h-9 w-20" />;
  }
  const authenticated = state.status === "authenticated";
  return (
    <Link
      href={authenticated ? "/account" : "/account/login"}
      className="flex h-9 items-center gap-1.5 px-2 text-small text-ink-2 transition-colors hover:text-foreground"
      title={authenticated ? state.buyer.companyName : undefined}
    >
      <CircleUserRound aria-hidden className="size-4" strokeWidth={1.5} />
      <span className="hidden sm:inline">{authenticated ? t("account") : t("signIn")}</span>
      <span className="sr-only sm:hidden">{authenticated ? t("account") : t("signIn")}</span>
    </Link>
  );
}
