import { getTranslations } from "next-intl/server";

/** Uczciwe oznaczenie danych przykładowych — włączane zmienną NEXT_PUBLIC_DEMO_NOTICE do czasu importu prawdziwej oferty. */
export async function DemoNotice() {
  if (process.env.NEXT_PUBLIC_DEMO_NOTICE !== "true") {
    return null;
  }
  const t = await getTranslations("site");
  return (
    <p className="bg-well px-5 py-2 text-center text-meta text-ink-2 md:px-8" role="note">
      {t("demoNotice")}
    </p>
  );
}
