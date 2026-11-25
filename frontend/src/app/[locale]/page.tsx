import { getTranslations, setRequestLocale } from "next-intl/server";
import { fetchCategories, fetchFacets, fetchProducts } from "@/lib/api";
import type { CatalogSort } from "@/lib/types";
import { CatalogToolbar } from "@/components/catalog/catalog-toolbar";
import { ProductCard } from "@/components/catalog/product-card";
import { Pagination } from "@/components/catalog/pagination";
import { PriceGate, PricingScope } from "@/components/catalog/pricing";
import { Link } from "@/i18n/navigation";

const SORTS: CatalogSort[] = ["RECOMMENDED", "NEWEST", "PRICE_ASC", "PRICE_DESC"];

const list = (value: string | string[] | undefined) => (value === undefined ? [] : [value].flat());
const one = (value: string | string[] | undefined) => list(value)[0];

export async function generateMetadata({ params, searchParams }: PageProps<"/[locale]">) {
  const { locale } = await params;
  const category = one((await searchParams).category);
  const t = await getTranslations({ locale, namespace: "catalog" });
  const categories = category ? await fetchCategories(locale).catch(() => []) : [];
  return { title: categories.find((c) => c.slug === category)?.name ?? t("title") };
}

export default async function CatalogPage({ params, searchParams }: PageProps<"/[locale]">) {
  const { locale } = await params;
  setRequestLocale(locale);
  const query = await searchParams;

  const category = one(query.category);
  const colors = list(query.color);
  const sizes = list(query.size);
  const q = one(query.q)?.slice(0, 100);
  const sortParam = one(query.sort) as CatalogSort | undefined;
  const sort = sortParam && SORTS.includes(sortParam) ? sortParam : "RECOMMENDED";
  const page = Math.max(1, Number.parseInt(one(query.page) ?? "1", 10) || 1);

  const [result, facets, categories] = await Promise.all([
    fetchProducts(locale, { category, colors, sizes, q, sort, page: page - 1 }),
    fetchFacets(locale),
    fetchCategories(locale),
  ]);
  const t = await getTranslations("catalog");
  const tSite = await getTranslations("site");
  const activeCategory = categories.find((c) => c.slug === category);
  const filtered = colors.length > 0 || sizes.length > 0 || Boolean(q);

  return (
    <div className="mx-auto max-w-[1600px] px-5 md:px-8">
      <div className="pb-5 pt-7 md:pt-9">
        <h1 className="text-display font-medium tracking-[-0.02em] text-balance">
          {activeCategory?.name ?? t("title")}
        </h1>
        <p className="mt-1.5 text-ink-2">
          {activeCategory?.description ?? t("lead")} <span className="text-ink-3">{tSite("priceNote")}.</span>
        </p>
      </div>

      <PriceGate className="mb-5" />

      <CatalogToolbar
        facets={facets}
        selectedColors={colors}
        selectedSizes={sizes}
        query={q ?? ""}
        sort={sort}
        resultsLabel={t("results", { count: result.totalItems })}
      />

      {result.items.length > 0 ? (
        <PricingScope productIds={result.items.map((product) => product.id)}>
          <ul className="grid grid-cols-2 gap-x-3 gap-y-10 pt-6 md:grid-cols-3 md:gap-x-4 lg:grid-cols-4 lg:gap-y-14">
            {result.items.map((product, index) => (
              <li key={product.id}>
                <ProductCard product={product} priority={index < 4} />
              </li>
            ))}
          </ul>
          <Pagination page={page} totalPages={result.totalPages} />
        </PricingScope>
      ) : (
        <div className="flex flex-col items-start gap-4 py-24">
          <p className="max-w-[52ch] text-ink-2">{filtered || category ? t("empty") : t("emptyCatalog")}</p>
          {(filtered || category) && (
            <Link href="/" className="underline underline-offset-4 hover:no-underline">
              {t("clearFilters")}
            </Link>
          )}
        </div>
      )}
    </div>
  );
}
