"use client";

import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { cn } from "@/lib/utils";

export function Pagination({ page, totalPages }: { page: number; totalPages: number }) {
  const t = useTranslations("catalog");
  const searchParams = useSearchParams();
  if (totalPages <= 1) return null;

  const hrefFor = (target: number) => {
    const query: Record<string, string | string[]> = {};
    for (const key of new Set(searchParams.keys())) {
      const values = searchParams.getAll(key);
      query[key] = values.length > 1 ? values : values[0];
    }
    if (target > 1) query.page = String(target);
    else delete query.page;
    return { pathname: "/" as const, query };
  };

  const linkClass = "px-3 py-2 underline-offset-4 hover:underline";
  return (
    <nav aria-label={t("pagination")} className="mt-16 flex items-center justify-between border-t border-line pt-6 text-small">
      {page > 1 ? (
        <Link href={hrefFor(page - 1)} className={cn(linkClass, "-ml-3")} rel="prev">
          ← {t("previous")}
        </Link>
      ) : (
        <span />
      )}
      <span className="tabular text-ink-3">{t("pageOf", { page, total: totalPages })}</span>
      {page < totalPages ? (
        <Link href={hrefFor(page + 1)} className={cn(linkClass, "-mr-3")} rel="next">
          {t("next")} →
        </Link>
      ) : (
        <span />
      )}
    </nav>
  );
}
