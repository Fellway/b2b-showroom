import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { fetchLegalDocument, fetchLegalVersion, fetchLegalVersions, NotFoundError } from "@/lib/api";
import type { LegalDocument, LegalVersionSummary } from "@/lib/types";
import { LegalMarkdown } from "./legal-markdown";

/**
 * Publiczna strona dokumentu prawnego (D25): obowiązująca wersja, a z `?version=<id>` — wersja archiwalna (link
 * z historii zgód w „Moim koncie”). Pod treścią historia opublikowanych wersji.
 */

type Slug = "terms" | "privacy";

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

async function load(locale: string, slug: Slug, versionId: string | undefined): Promise<LegalDocument> {
  try {
    if (versionId && UUID.test(versionId)) {
      const version = await fetchLegalVersion(locale, versionId);
      if (version.type === (slug === "terms" ? "TERMS" : "PRIVACY_POLICY")) return version;
      notFound();
    }
    return await fetchLegalDocument(locale, slug);
  } catch (error) {
    if (error instanceof NotFoundError) notFound();
    throw error;
  }
}

const versionParam = (value: string | string[] | undefined) => (typeof value === "string" ? value : undefined);

export async function legalMetadata(locale: string, slug: Slug): Promise<Metadata> {
  const t = await getTranslations({ locale, namespace: "legal" });
  return { title: t(slug === "terms" ? "termsTitle" : "privacyTitle") };
}

export async function LegalDocumentPage({
  locale,
  slug,
  searchParams,
}: {
  locale: string;
  slug: Slug;
  searchParams: Record<string, string | string[] | undefined>;
}) {
  setRequestLocale(locale);
  const requested = versionParam(searchParams.version);
  const [document, versions, t] = await Promise.all([
    load(locale, slug, requested),
    fetchLegalVersions(locale, slug).catch((): LegalVersionSummary[] => []),
    getTranslations("legal"),
  ]);
  const current = versions[0];
  const archived = current !== undefined && current.id !== document.versionId;
  const date = (iso: string) => new Intl.DateTimeFormat(locale, { dateStyle: "long" }).format(new Date(iso));

  return (
    <article className="mx-auto w-full max-w-[720px] px-5 pb-24 pt-10 md:pt-14">
      <header className="border-b border-line pb-6">
        <h1 className="text-title font-medium text-balance md:text-[1.75rem] md:leading-[2.25rem]">{document.title}</h1>
        <p className="mt-2 text-small text-ink-3">
          {t("versionMeta", { number: document.versionNumber, date: date(document.publishedAt) })}
        </p>
        {archived && (
          <p className="mt-4 border-l-2 border-foreground bg-well px-4 py-3 text-small">
            {t("archived")}{" "}
            <Link href={`/${slug}`} className="underline underline-offset-4 hover:no-underline">
              {t("seeCurrent")}
            </Link>
          </p>
        )}
        {document.language.toLowerCase() !== locale && <p className="mt-3 text-small text-ink-3">{t("notTranslated")}</p>}
      </header>

      <LegalMarkdown source={document.body} className="mt-8 text-body" />

      {versions.length > 1 && (
        <section aria-labelledby="legal-versions" className="mt-14 border-t border-line pt-6">
          <h2 id="legal-versions" className="text-small font-medium">
            {t("history")}
          </h2>
          <ul className="mt-3 grid gap-1.5 text-small">
            {versions.map((version) => (
              <li key={version.id} className="flex flex-wrap items-baseline gap-x-3">
                {version.id === document.versionId ? (
                  <span aria-current="page" className="font-medium">
                    {t("versionLabel", { number: version.versionNumber ?? 0 })}
                  </span>
                ) : (
                  <Link
                    href={{ pathname: `/${slug}`, query: version.id === current?.id ? undefined : { version: version.id } }}
                    className="underline underline-offset-4 hover:no-underline"
                  >
                    {t("versionLabel", { number: version.versionNumber ?? 0 })}
                  </Link>
                )}
                <span className="text-ink-3">{version.publishedAt && date(version.publishedAt)}</span>
                {version.id === current?.id && <span className="text-ink-3">{t("currentBadge")}</span>}
              </li>
            ))}
          </ul>
        </section>
      )}
    </article>
  );
}
