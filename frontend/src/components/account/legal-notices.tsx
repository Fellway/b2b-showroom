"use client";

import { useLocale, useTranslations } from "next-intl";
import { useEffect, useState } from "react";
import { Dialog as DialogPrimitive } from "radix-ui";
import { Link, usePathname } from "@/i18n/navigation";
import { BuyerApiError } from "@/lib/buyer/api";
import { buyerLegal, useBuyerLegal } from "@/lib/buyer/legal";
import { buyerSession } from "@/lib/buyer/session";
import { LEGAL_SLUG, type LegalDocument, type PendingDocument } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/**
 * Zmiany dokumentów prawnych dla zalogowanego kupca (D25):
 * - nowa wersja regulaminu wymagająca akceptacji → okno, którego nie da się pominąć (ceny i zestawienia są do tego
 *   czasu zablokowane także w API); kupiec może zamiast tego wylogować się albo przejść do „Mojego konta”
 *   (pobranie danych, usunięcie konta — tam okno się nie pojawia, a akceptacja jest w treści strony);
 * - nowa wersja polityki prywatności → pasek z informacją („Rozumiem” zapisuje przyjęcie do wiadomości).
 */
const EXEMPT_PATHS = ["/account", "/terms", "/privacy"];

/** Tytuł i „co się zmieniło” dla wersji oczekujących na akceptację (publiczne API, w języku strony). */
export function usePendingDocuments(pending: PendingDocument[]) {
  const locale = useLocale();
  const [documents, setDocuments] = useState<Record<string, LegalDocument>>({});
  const key = pending.map((p) => p.versionId).join(",");
  useEffect(() => {
    if (!key) return;
    let cancelled = false;
    Promise.all(
      key.split(",").map((id) =>
        fetch(`/api/v1/legal/versions/${id}?lang=${locale}`, { headers: { Accept: "application/json" } })
          .then((response) => (response.ok ? (response.json() as Promise<LegalDocument>) : null))
          .catch(() => null),
      ),
    ).then((loaded) => {
      if (cancelled) return;
      setDocuments(Object.fromEntries(loaded.filter((d): d is LegalDocument => d !== null).map((d) => [d.versionId, d])));
    });
    return () => {
      cancelled = true;
    };
  }, [key, locale]);
  return documents;
}

/** Akceptacja wszystkich oczekujących wersji; `outdated` = w międzyczasie opublikowano kolejną (stan odświeżony). */
export function useAcceptPending() {
  const locale = useLocale();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<"outdated" | "generic" | null>(null);
  async function accept(versionIds: string[]) {
    setPending(true);
    setError(null);
    try {
      await buyerLegal.accept(versionIds, locale);
    } catch (e) {
      const outdated = e instanceof BuyerApiError && e.problem.code === "identity.buyer.legal-version-outdated";
      setError(outdated ? "outdated" : "generic");
      if (outdated) await buyerLegal.load();
    } finally {
      setPending(false);
    }
  }
  return { accept, pending, error };
}

export function LegalNotices() {
  const state = useBuyerLegal();
  const pathname = usePathname();
  if (state.status !== "ready") return null;
  const { pending } = state.data;
  const exempt = EXEMPT_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`));
  if (pending.some((p) => p.blocking)) {
    return exempt ? null : <TermsUpdateDialog pending={pending} />;
  }
  if (pending.length > 0 && !exempt) return <PrivacyNotice pending={pending} />;
  return null;
}

function TermsUpdateDialog({ pending }: { pending: PendingDocument[] }) {
  const t = useTranslations("legal.update");
  const documents = usePendingDocuments(pending);
  const { accept, pending: sending, error } = useAcceptPending();
  const [checked, setChecked] = useState(false);
  const firstTime = pending.some((p) => p.blocking && p.firstTime);

  return (
    <DialogPrimitive.Root open>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-black/30" />
        <DialogPrimitive.Content
          onEscapeKeyDown={(event) => event.preventDefault()}
          onPointerDownOutside={(event) => event.preventDefault()}
          onInteractOutside={(event) => event.preventDefault()}
          className="fixed left-1/2 top-1/2 z-50 grid max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-[560px] -translate-x-1/2 -translate-y-1/2 gap-5 overflow-y-auto bg-background p-6 shadow-menu outline-none md:p-8"
        >
          <DialogPrimitive.Title className="text-title font-medium text-balance">
            {firstTime ? t("titleFirst") : t("title")}
          </DialogPrimitive.Title>
          <DialogPrimitive.Description className="text-ink-2">{firstTime ? t("leadFirst") : t("lead")}</DialogPrimitive.Description>

          <ul className="grid gap-4 border-y border-line py-5">
            {pending.map((item) => {
              const document = documents[item.versionId];
              return (
                <li key={item.versionId} className="grid gap-1">
                  <p className="font-medium">
                    {document?.title ?? t(item.type === "TERMS" ? "terms" : "privacy")}{" "}
                    <span className="font-normal text-ink-3">{t("version", { number: item.versionNumber })}</span>
                  </p>
                  {document?.changeSummary && <p className="text-small text-ink-2">{document.changeSummary}</p>}
                  <Link
                    href={{ pathname: `/${LEGAL_SLUG[item.type]}`, query: { version: item.versionId } }}
                    target="_blank"
                    className="w-fit text-small underline underline-offset-4 hover:no-underline"
                  >
                    {t("read")}
                  </Link>
                </li>
              );
            })}
          </ul>

          <label className="flex items-start gap-3 text-small">
            <input
              type="checkbox"
              checked={checked}
              onChange={(e) => setChecked(e.target.checked)}
              className="mt-0.5 size-4 shrink-0 accent-foreground"
            />
            <span>{t("confirm")}</span>
          </label>
          {error && (
            <p role="alert" className="text-small text-danger">
              {t(error === "outdated" ? "outdated" : "error")}
            </p>
          )}

          <div className="grid gap-3">
            <Button disabled={!checked || sending} onClick={() => accept(pending.map((p) => p.versionId))}>
              {sending ? t("accepting") : t("accept")}
            </Button>
            <p className="text-small text-ink-3">
              {t.rich("alternatives", {
                account: (chunks) => (
                  <Link href="/account" className="text-ink-2 underline underline-offset-4 hover:text-foreground">
                    {chunks}
                  </Link>
                ),
                logout: (chunks) => (
                  <button
                    type="button"
                    onClick={() => void buyerSession.logout()}
                    className="text-ink-2 underline underline-offset-4 hover:text-foreground"
                  >
                    {chunks}
                  </button>
                ),
              })}
            </p>
          </div>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}

function PrivacyNotice({ pending }: { pending: PendingDocument[] }) {
  const t = useTranslations("legal.notice");
  const { accept, pending: sending, error } = useAcceptPending();
  const privacy = pending.find((p) => p.type === "PRIVACY_POLICY");
  if (!privacy) return null;
  return (
    <section
      aria-label={t("label")}
      className={cn("border-b border-line bg-well")}
    >
      <div className="mx-auto flex max-w-[1600px] flex-col gap-3 px-5 py-3 text-small sm:flex-row sm:items-center sm:justify-between md:px-8">
        <p>
          {t("text", { number: privacy.versionNumber })}{" "}
          <Link
            href={{ pathname: "/privacy", query: { version: privacy.versionId } }}
            className="underline underline-offset-4 hover:no-underline"
          >
            {t("read")}
          </Link>
          {error && <span className="ml-2 text-danger">{t("error")}</span>}
        </p>
        <Button variant="secondary" size="sm" disabled={sending} onClick={() => accept([privacy.versionId])}>
          {t("dismiss")}
        </Button>
      </div>
    </section>
  );
}
