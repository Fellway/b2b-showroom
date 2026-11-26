import { LegalDocumentPage, legalMetadata } from "@/components/legal/legal-document-page";

export async function generateMetadata({ params }: PageProps<"/[locale]/privacy">) {
  const { locale } = await params;
  return legalMetadata(locale, "privacy");
}

export default async function Page({ params, searchParams }: PageProps<"/[locale]/privacy">) {
  const { locale } = await params;
  return <LegalDocumentPage locale={locale} slug="privacy" searchParams={await searchParams} />;
}
