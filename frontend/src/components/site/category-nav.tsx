"use client";

import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { Link, usePathname } from "@/i18n/navigation";
import type { Category } from "@/lib/types";
import { cn } from "@/lib/utils";

export function CategoryNav({ categories, className }: { categories: Category[]; className?: string }) {
  const t = useTranslations("site");
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const onCatalog = pathname === "/";
  const active = onCatalog ? (searchParams.get("category") ?? "") : null;

  const items = [{ slug: "", name: t("allProducts") }, ...categories.map((c) => ({ slug: c.slug, name: c.name }))];

  return (
    <nav aria-label={t("context")} className={cn("items-stretch gap-6 text-small", className)}>
      {items.map((item) => {
        const isActive = active === item.slug;
        return (
          <Link
            key={item.slug || "all"}
            href={item.slug ? { pathname: "/", query: { category: item.slug } } : "/"}
            aria-current={isActive ? "page" : undefined}
            className={cn(
              "relative flex shrink-0 items-center whitespace-nowrap py-3 text-ink-2 transition-colors hover:text-foreground",
              "after:absolute after:inset-x-0 after:bottom-0 after:h-px after:origin-left after:scale-x-0 after:bg-foreground after:transition-transform after:duration-300 after:ease-(--ease-out-expo)",
              isActive && "text-foreground after:scale-x-100",
            )}
          >
            {item.name}
          </Link>
        );
      })}
    </nav>
  );
}
