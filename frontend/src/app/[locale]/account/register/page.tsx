import type { Metadata } from "next";
import { Suspense } from "react";
import { connection } from "next/server";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { RegisterForm } from "@/components/account/register-form";

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
  return (
    <Suspense>
      <RegisterForm />
    </Suspense>
  );
}