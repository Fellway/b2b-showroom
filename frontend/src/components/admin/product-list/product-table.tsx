"use client";

import Image from "next/image";
import Link from "next/link";
import { ImageOff } from "lucide-react";
import { useRouter } from "next/navigation";
import { StatusLabel } from "@/components/admin/ui/status-label";
import { Table, Td, Th, Tr } from "@/components/admin/ui/table";
import { formatDate, type ProductListItem } from "@/lib/admin/product-list";
import { PriceCell } from "./price-cell";
import { StatusAction } from "./status-action";

export function ProductTable({ items }: { items: ProductListItem[] }) {
  return (
    <>
      <ProductCards items={items} />
      <ProductRows items={items} />
    </>
  );
}

/**
 * Telefon: karta modelu z szybkimi zmianami — publikacja / ukrycie i cena hurtowa bez wchodzenia w edytor.
 * Pełna edycja (warianty, zdjęcia, tłumaczenia) zostaje na komputerze.
 */
function ProductCards({ items }: { items: ProductListItem[] }) {
  return (
    <ul className="divide-y divide-line border-y border-line md:hidden">
      {items.map((product) => {
        const href = `/admin/products/${product.id}`;
        return (
          <li key={product.id} className="flex gap-3 py-3">
            <Link href={href} className="relative flex h-20 w-14 shrink-0 items-center justify-center bg-well" aria-hidden tabIndex={-1}>
              {product.cover ? (
                <Image src={product.cover.url} alt="" fill sizes="56px" className="object-cover" />
              ) : (
                <ImageOff className="size-4 text-ink-3" strokeWidth={1.5} />
              )}
            </Link>
            <div className="flex min-w-0 flex-1 flex-col gap-1">
              <Link href={href} className="font-medium underline-offset-4 active:underline">
                {product.name}
              </Link>
              <p className="tabular text-meta text-ink-3">
                {product.styleCode} · {product.categoryName}
              </p>
              <div className="flex items-center justify-between gap-2 text-meta">
                <StatusLabel status={product.status} />
                <PriceCell product={product} />
              </div>
              <div className="flex items-center justify-between gap-2 text-meta">
                <span className="min-w-0 truncate">
                  <Gaps product={product} inline />
                </span>
                <StatusAction product={product} />
              </div>
            </div>
          </li>
        );
      })}
    </ul>
  );
}

function ProductRows({ items }: { items: ProductListItem[] }) {
  const router = useRouter();
  return (
    <Table className="hidden md:table">
      <thead>
        <tr>
          <Th className="w-12">
            <span className="sr-only">Zdjęcie</span>
          </Th>
          <Th>Model</Th>
          <Th>Kategoria</Th>
          <Th>Status</Th>
          <Th numeric className="w-48">
            Cena hurtowa
          </Th>
          <Th numeric>Warianty</Th>
          <Th>Braki</Th>
          <Th>Zmieniony</Th>
          <Th>
            <span className="sr-only">Akcje</span>
          </Th>
        </tr>
      </thead>
      <tbody>
        {items.map((product) => {
          const href = `/admin/products/${product.id}`;
          return (
            <Tr key={product.id} interactive onClick={() => router.push(href)}>
              <Td className="py-2">
                <div className="relative flex h-12 w-8 items-center justify-center bg-well">
                  {product.cover ? (
                    <Image src={product.cover.url} alt="" fill sizes="32px" className="object-cover" />
                  ) : (
                    <ImageOff aria-label="Brak zdjęć" className="size-3.5 text-ink-3" strokeWidth={1.5} />
                  )}
                </div>
              </Td>
              <Td>
                <Link href={href} onClick={(e) => e.stopPropagation()} className="font-medium underline-offset-4 hover:underline">
                  {product.name}
                </Link>
                <p className="tabular text-meta text-ink-3">{product.styleCode}</p>
              </Td>
              <Td className="text-ink-2">{product.categoryName}</Td>
              <Td>
                <StatusLabel status={product.status} />
              </Td>
              <Td numeric>
                <PriceCell product={product} />
              </Td>
              <Td numeric className="text-ink-2">
                <span title={`${product.activeVariantCount} aktywnych z ${product.variantCount}`}>
                  {product.activeVariantCount}/{product.variantCount}
                </span>
              </Td>
              <Td className="text-meta">
                <Gaps product={product} />
              </Td>
              <Td className="tabular whitespace-nowrap text-meta text-ink-3">{formatDate(product.updatedAt)}</Td>
              <Td className="text-right">
                <StatusAction product={product} />
              </Td>
            </Tr>
          );
        })}
      </tbody>
    </Table>
  );
}

/** Co trzeba uzupełnić w modelu: tłumaczenia (wklejane z zewnątrz), zdjęcia, warianty. */
function Gaps({ product, inline }: { product: ProductListItem; inline?: boolean }) {
  const gaps = [
    product.missingLanguages.length > 0 && `tłumaczenia: ${product.missingLanguages.map((code) => code.toUpperCase()).join(", ")}`,
    !product.cover && "zdjęcia",
    product.variantCount === 0 && "warianty",
  ].filter(Boolean) as string[];
  if (gaps.length === 0) return <span className="text-ink-2">komplet</span>;
  if (inline) return <span className="text-ink-3">brak: {gaps.join(", ")}</span>;
  return (
    <ul className="flex flex-col text-ink-3">
      {gaps.map((gap) => (
        <li key={gap}>brak: {gap}</li>
      ))}
    </ul>
  );
}
