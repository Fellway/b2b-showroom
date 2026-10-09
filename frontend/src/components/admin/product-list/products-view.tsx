"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Plus } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { PageBody, PageHeader } from "@/components/admin/ui/page-header";
import { EmptyState, ErrorState, LoadingRows } from "@/components/admin/ui/states";
import { errorMessage } from "@/lib/admin/errors";
import { useProductList, type ProductListFilter } from "@/lib/admin/product-list";
import { cn } from "@/lib/utils";
import { ListPagination } from "./list-pagination";
import { ProductFilters } from "./product-filters";
import { ProductTable } from "./product-table";

const modelCount = (n: number) => {
  if (n === 1) return "1 model";
  const tens = n % 100;
  const ones = n % 10;
  return ones >= 2 && ones <= 4 && (tens < 12 || tens > 14) ? `${n} modele` : `${n} modeli`;
};

export function ProductsView() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const filter: ProductListFilter = {
    q: searchParams.get("q") ?? undefined,
    status: searchParams.get("status") ?? undefined,
    categoryId: searchParams.get("categoryId") ?? undefined,
    page: Math.max(0, Number.parseInt(searchParams.get("page") ?? "0", 10) || 0),
  };
  const list = useProductList(filter);

  const update = (next: Partial<ProductListFilter>) => {
    const merged = { ...filter, page: 0, ...next };
    const params = new URLSearchParams();
    if (merged.q) params.set("q", merged.q);
    if (merged.status) params.set("status", merged.status);
    if (merged.categoryId) params.set("categoryId", merged.categoryId);
    if (merged.page > 0) params.set("page", String(merged.page));
    const text = params.toString();
    router.replace(text ? `${pathname}?${text}` : pathname, { scroll: false });
  };

  const filtered = Boolean(filter.q || filter.status || filter.categoryId);

  return (
    <>
      <PageHeader
        title="Modele"
        meta={list.data ? <span className="tabular">{modelCount(list.data.totalItems)}</span> : undefined}
        actions={
          <Link href="/admin/products/new" className={cn(buttonVariants({ variant: "primary", size: "md" }))}>
            <Plus aria-hidden strokeWidth={1.5} />
            Nowy model
          </Link>
        }
      />
      <PageBody>
        <ProductFilters filter={filter} onChange={update} />
        {list.isPending ? (
          <LoadingRows />
        ) : list.isError ? (
          <ErrorState message={errorMessage(list.error)} onRetry={() => list.refetch()} />
        ) : list.data.items.length === 0 ? (
          filtered ? (
            <EmptyState title="Żaden model nie pasuje do filtrów.">Zmień wyszukiwanie albo wyczyść filtry.</EmptyState>
          ) : (
            <EmptyState
              title="Katalog nie ma jeszcze modeli."
              action={
                <Link href="/admin/products/new" className="underline underline-offset-4 hover:no-underline">
                  Dodaj pierwszy model
                </Link>
              }
            >
              Model zaczyna się od danych podstawowych; kolory, rozmiary z EAN i zdjęcia dodasz w kolejnych krokach.
            </EmptyState>
          )
        ) : (
          <div className={cn("transition-opacity", list.isPlaceholderData && "opacity-60")} aria-busy={list.isFetching}>
            <ProductTable items={list.data.items} />
            <ListPagination page={list.data.page} totalPages={list.data.totalPages} onPage={(page) => update({ page })} />
          </div>
        )}
      </PageBody>
    </>
  );
}
