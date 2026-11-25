import type { Metadata } from "next";
import { Suspense } from "react";
import { connection } from "next/server";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { LoginForm } from "@/components/account/login-form";

export async function generateMetadata({ params }: PageProps<"/[locale]/account/login">): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "account.titles" });
  return { title: t("login"), robots: { index: false, follow: false } };
}

export default async function Page({ params }: PageProps<"/[locale]/account/login">) {
  // strony konta są indywidualne — renderowane na żądanie (nawigacja i kontakty z aktualnego API, nie z chwili buildu)
  await connection();
  const { locale } = await params;
  setRequestLocale(locale);
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}