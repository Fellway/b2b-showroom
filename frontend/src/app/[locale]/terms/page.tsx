import { LegalDocumentPage, legalMetadata } from "@/components/legal/legal-document-page";

export async function generateMetadata({ params }: PageProps<"/[locale]/terms">) {
  const { locale } = await params;
  return legalMetadata(locale, "terms");
}

export default async function Page({ params, searchParams }: PageProps<"/[locale]/terms">) {
  const { locale } = await params;
  return <LegalDocumentPage locale={locale} slug="terms" searchParams={await searchParams} />;
}
