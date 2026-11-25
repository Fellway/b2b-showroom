import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { RegisterForm } from "@/components/account/register-form";
import { suggestedCountry } from "@/lib/request-country";

export async function generateMetadata({ params }: PageProps<"/[locale]/account/register">): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "account.titles" });
  return { title: t("register"), robots: { index: false, follow: false } };
}

export default async function Page({ params }: PageProps<"/[locale]/account/register">) {
  // strony konta są indywidualne — renderowane na żądanie (nawigacja i kontakty z aktualnego API, nie z chwili buildu)
  await connection();
  const { locale } = await params;
  setRequestLocale(locale);
  // podpowiedź kraju firmy i numeru kierunkowego na podstawie żądania (zob. lib/request-country.ts)
  const country = await suggestedCountry(locale);
  return (
    <Suspense>
      <RegisterForm suggestedCountry={country} />
    </Suspense>
  );
}
