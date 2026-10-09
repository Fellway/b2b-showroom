import type { Metadata } from "next";
import { Golos_Text } from "next/font/google";
import { notFound } from "next/navigation";
import { hasLocale, NextIntlClientProvider } from "next-intl";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { routing } from "@/i18n/routing";
import { fetchCategories, fetchRepresentatives } from "@/lib/api";
import { SiteHeader } from "@/components/site/site-header";
import { SiteFooter } from "@/components/site/site-footer";
import { DemoNotice } from "@/components/site/demo-notice";
import { PageViewTracker } from "@/components/site/page-view-tracker";
import { ConsentBanner } from "@/components/site/consent-banner";
import { SelectionDrawer } from "@/components/selection/selection-drawer";
import "../globals.css";

const golos = Golos_Text({
  subsets: ["latin", "latin-ext", "cyrillic", "cyrillic-ext"],
  weight: ["400", "500"],
  variable: "--font-golos",
  display: "swap",
});

export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }));
}

export async function generateMetadata({ params }: LayoutProps<"/[locale]">): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "meta" });
  return {
    title: { default: t("title"), template: `%s · Fabian's Style` },
    description: t("description"),
    icons: { icon: "/brand/favicon.png" },
  };
}

export default async function LocaleLayout({ children, params }: LayoutProps<"/[locale]">) {
  const { locale } = await params;
  if (!hasLocale(routing.locales, locale)) {
    notFound();
  }
  setRequestLocale(locale);

  const [categories, representatives] = await Promise.all([
    fetchCategories(locale).catch(() => []),
    fetchRepresentatives(locale).catch(() => []),
  ]);
  const t = await getTranslations("site");

  return (
    <html lang={locale} className={golos.variable}>
      <body className="flex min-h-dvh flex-col">
        <NextIntlClientProvider>
          <a
            href="#content"
            className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:bg-foreground focus:px-4 focus:py-2 focus:text-background"
          >
            {t("skipToContent")}
          </a>
          <DemoNotice />
          <SiteHeader categories={categories.filter((c) => c.parentId === null)} />
          <main id="content" className="flex-1">
            {children}
          </main>
          <SiteFooter representatives={representatives} />
          <SelectionDrawer representatives={representatives} />
          <PageViewTracker />
          <ConsentBanner />
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
