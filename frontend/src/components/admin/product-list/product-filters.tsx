"use client";

import { Search } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Select } from "@/components/admin/ui/field";
import { PRODUCT_STATUS_LABEL, type ProductStatus } from "@/components/admin/ui/status-label";
import { categoryName, useCategories } from "@/lib/admin/dictionaries";
import type { ProductListFilter } from "@/lib/admin/product-list";

const STATUSES = Object.keys(PRODUCT_STATUS_LABEL) as ProductStatus[];

/** Wyszukiwarka i filtry listy modeli; stan żyje w URL (link do przefiltrowanej listy da się zachować). */
export function ProductFilters({
  filter,
  onChange,
}: {
  filter: ProductListFilter;
  onChange: (next: Partial<ProductListFilter>) => void;
}) {
  const categories = useCategories();
  const [search, setSearch] = useState(filter.q ?? "");

  const submit = (event: FormEvent) => {
    event.preventDefault();
    onChange({ q: search.trim() || undefined });
  };

  return (
    <div className="grid grid-cols-2 items-end gap-3 pb-6 md:flex md:flex-wrap">
      <form onSubmit={submit} role="search" className="relative col-span-2 w-full md:max-w-sm">
        <Search aria-hidden className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink-3" strokeWidth={1.5} />
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onBlur={() => (search.trim() || undefined) !== filter.q && onChange({ q: search.trim() || undefined })}
          maxLength={100}
          placeholder="Szukaj: nazwa, kod modelu, SKU lub EAN"
          aria-label="Szukaj modelu"
          className="h-10 w-full border border-edge bg-background pl-9 pr-3 text-small outline-none transition-colors placeholder:text-ink-3 hover:border-foreground focus-visible:border-foreground focus-visible:outline-none"
        />
      </form>
      <div className="md:w-44">
        <Select aria-label="Status" value={filter.status ?? ""} onChange={(e) => onChange({ status: e.target.value || undefined })}>
          <option value="">Wszystkie statusy</option>
          {STATUSES.map((status) => (
            <option key={status} value={status}>
              {PRODUCT_STATUS_LABEL[status]}
            </option>
          ))}
        </Select>
      </div>
      <div className="md:w-56">
        <Select
          aria-label="Kategoria"
          value={filter.categoryId ?? ""}
          onChange={(e) => onChange({ categoryId: e.target.value || undefined })}
        >
          <option value="">Wszystkie kategorie</option>
          {(categories.data ?? []).map((category) => (
            <option key={category.id} value={category.id}>
              {category.parentId ? "— " : ""}
              {categoryName(category)}
            </option>
          ))}
        </Select>
      </div>
      {(filter.q || filter.status || filter.categoryId) && (
        <button
          type="button"
          onClick={() => {
            setSearch("");
            onChange({ q: undefined, status: undefined, categoryId: undefined });
          }}
          className="col-span-2 h-10 justify-self-start px-1 text-meta underline underline-offset-4 hover:no-underline"
        >
          Wyczyść filtry
        </button>
      )}
    </div>
  );
}
