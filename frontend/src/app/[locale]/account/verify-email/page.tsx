import type { Metadata } from "next";
import { connection } from "next/server";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { VerifyEmail } from "@/components/account/verify-email";

export async function generateMetadata({ params }: PageProps<"/[locale]/account/verify-email">): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "account.titles" });
  return { title: t("verify"), robots: { index: false, follow: false } };
}

export default async function Page({ params }: PageProps<"/[locale]/account/verify-email">) {
  // strony konta są indywidualne — renderowane na żądanie (nawigacja i kontakty z aktualnego API, nie z chwili buildu)
  await connection();
  const { locale } = await params;
  setRequestLocale(locale);
  return <VerifyEmail />;
}