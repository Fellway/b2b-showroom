import Image from "next/image";
import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import type { ProductCard as ProductCardData } from "@/lib/types";
import { InSelectionMark } from "./in-selection-mark";
import { CardPrice } from "./pricing";

export async function ProductCard({ product, priority }: { product: ProductCardData; priority?: boolean }) {
  const t = await getTranslations("catalog");

  return (
    <Link href={`/p/${product.slug}`} className="group block">
      <div className="relative aspect-[2/3] overflow-hidden bg-well">
        {product.cover && (
          <Image
            src={product.cover.url}
            alt=""
            fill
            priority={priority}
            sizes="(min-width: 1024px) 25vw, (min-width: 768px) 33vw, 50vw"
            className="object-cover transition-transform duration-700 ease-(--ease-out-expo) group-hover:scale-[1.025]"
          />
        )}
      </div>
      <div className="flex flex-col gap-1 pt-3">
        <h2 className="text-small font-medium underline-offset-4 group-hover:underline">{product.name}</h2>
        <div className="flex items-center gap-2 text-meta text-ink-3">
          <span>{product.styleCode}</span>
          <span aria-hidden>·</span>
          <span className="flex items-center gap-1" aria-label={product.colors.map((c) => c.name).join(", ")}>
            {product.colors.slice(0, 5).map((color) => (
              <span
                key={color.id}
                aria-hidden
                className="inline-block size-2.5 border border-black/15"
                style={{ backgroundColor: color.hex ?? "transparent" }}
              />
            ))}
            <span className="ml-0.5">{t("colorsCount", { count: product.colors.length })}</span>
          </span>
          <InSelectionMark productId={product.id} />
        </div>
        {/* ceny dociąga przeglądarka zalogowanego kupca (D23) — strona katalogu zostaje wspólna dla wszystkich */}
        <CardPrice productId={product.id} />
      </div>
    </Link>
  );
}
