import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import { fetchProduct, fetchRepresentatives, NotFoundError } from "@/lib/api";
import { ProductView } from "@/components/catalog/product-view";

async function load(locale: string, slug: string) {
  try {
    return await fetchProduct(locale, slug);
  } catch (error) {
    if (error instanceof NotFoundError) notFound();
    throw error;
  }
}

export async function generateMetadata({ params }: PageProps<"/[locale]/p/[slug]">) {
  const { locale, slug } = await params;
  const product = await load(locale, slug);
  return {
    title: product.name,
    description: product.description ?? undefined,
    openGraph: product.images[0] ? { images: [{ url: product.images[0].url }] } : undefined,
  };
}

export default async function ProductPage({ params }: PageProps<"/[locale]/p/[slug]">) {
  const { locale, slug } = await params;
  setRequestLocale(locale);
  const [product, representatives] = await Promise.all([
    load(locale, slug),
    fetchRepresentatives(locale).catch(() => []),
  ]);
  return <ProductView product={product} representatives={representatives} />;
}
