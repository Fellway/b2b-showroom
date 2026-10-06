import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";

export default async function NotFound() {
  const t = await getTranslations("errors");
  return (
    <section className="mx-auto flex max-w-xl flex-col items-start gap-4 px-5 py-24 md:px-8">
      <h1 className="text-title font-medium text-balance">{t("notFoundTitle")}</h1>
      <p className="text-ink-2">{t("notFoundLead")}</p>
      <Link href="/" className="mt-2 underline hover:no-underline">
        {t("notFoundBack")}
      </Link>
    </section>
  );
}
