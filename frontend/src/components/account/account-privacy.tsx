"use client";

import { useLocale, useTranslations } from "next-intl";
import { Download } from "lucide-react";
import { useState, type FormEvent, type ReactNode } from "react";
import { Link } from "@/i18n/navigation";
import { BuyerApiError, buyerApi } from "@/lib/buyer/api";
import { useBuyerLegal } from "@/lib/buyer/legal";
import { LEGAL_SLUG, type PendingDocument } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { FormMessage, SubmitButton, TextField } from "./fields";
import { useAcceptPending, usePendingDocuments } from "./legal-notices";

/**
 * „Moje konto” — prawa z RODO i dokumenty (D25): dokumenty do zaakceptowania, historia zgód, pobranie danych
 * (art. 15 i 20) i usunięcie konta (art. 17).
 */

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="border-t border-line pt-5">
      <h2 className="text-body font-medium">{title}</h2>
      <div className="mt-5">{children}</div>
    </section>
  );
}

/** Na stronie konta okno akceptacji się nie pojawia — tu kupiec akceptuje nową wersję w treści strony. */
export function PendingDocumentsPanel({ pending }: { pending: PendingDocument[] }) {
  const t = useTranslations("legal.update");
  const documents = usePendingDocuments(pending);
  const { accept, pending: sending, error } = useAcceptPending();
  const [checked, setChecked] = useState(false);
  const blocking = pending.some((p) => p.blocking);

  return (
    <section aria-labelledby="pending-documents" className="mt-8 border border-l-2 border-line border-l-foreground px-5 py-5">
      <h2 id="pending-documents" className="font-medium">
        {blocking ? t("title") : t("noticeTitle")}
      </h2>
      <p className="mt-1 max-w-[72ch] text-small text-ink-2">{blocking ? t("lead") : t("noticeLead")}</p>
      <ul className="mt-4 grid gap-3">
        {pending.map((item) => {
          const document = documents[item.versionId];
          return (
            <li key={item.versionId} className="text-small">
              <Link
                href={{ pathname: `/${LEGAL_SLUG[item.type]}`, query: { version: item.versionId } }}
                target="_blank"
                className="font-medium underline underline-offset-4 hover:no-underline"
              >
                {document?.title ?? t(item.type === "TERMS" ? "terms" : "privacy")}
              </Link>{" "}
              <span className="text-ink-3">{t("version", { number: item.versionNumber })}</span>
              {document?.changeSummary && <p className="mt-0.5 text-ink-2">{document.changeSummary}</p>}
            </li>
          );
        })}
      </ul>
      {blocking && (
        <label className="mt-4 flex items-start gap-3 text-small">
          <input
            type="checkbox"
            checked={checked}
            onChange={(e) => setChecked(e.target.checked)}
            className="mt-0.5 size-4 shrink-0 accent-foreground"
          />
          <span>{t("confirm")}</span>
        </label>
      )}
      {error && (
        <p role="alert" className="mt-3 text-small text-danger">
          {t(error === "outdated" ? "outdated" : "error")}
        </p>
      )}
      <Button
        className="mt-4"
        variant={blocking ? "primary" : "secondary"}
        disabled={(blocking && !checked) || sending}
        onClick={() => accept(pending.map((p) => p.versionId))}
      >
        {sending ? t("accepting") : blocking ? t("accept") : t("acknowledge")}
      </Button>
    </section>
  );
}

export function PrivacySection({ onDeleted }: { onDeleted: () => void }) {
  const t = useTranslations("account.privacy");
  return (
    <Section title={t("title")}>
      <div className="grid gap-8">
        <ConsentHistory />
        <DataExport />
        <DeleteAccount onDeleted={onDeleted} />
      </div>
    </Section>
  );
}

function ConsentHistory() {
  const t = useTranslations("account.privacy");
  const locale = useLocale();
  const legal = useBuyerLegal();
  const dateFormat = new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short" });

  return (
    <div>
      <h3 className="text-small font-medium">{t("history")}</h3>
      <p className="mt-1 text-meta text-ink-3">
        {t.rich("documents", {
          terms: (chunks) => (
            <Link href="/terms" className="underline underline-offset-4 hover:text-foreground">
              {chunks}
            </Link>
          ),
          privacy: (chunks) => (
            <Link href="/privacy" className="underline underline-offset-4 hover:text-foreground">
              {chunks}
            </Link>
          ),
        })}
      </p>
      {legal.status === "ready" ? (
        legal.data.history.length === 0 ? (
          <p className="mt-3 text-small text-ink-2">{t("historyEmpty")}</p>
        ) : (
          <ul className="mt-3 divide-y divide-line border-y border-line text-small">
            {legal.data.history.map((consent) => (
              <li key={`${consent.documentVersionId}-${consent.acceptedAt}`} className="flex flex-wrap items-baseline gap-x-3 py-2.5">
                <Link
                  href={{ pathname: `/${LEGAL_SLUG[consent.documentType]}`, query: { version: consent.documentVersionId } }}
                  className="underline underline-offset-4 hover:no-underline"
                >
                  {t(consent.documentType === "TERMS" ? "termsVersion" : "privacyVersion", { number: consent.versionNumber })}
                </Link>
                <span className="text-ink-2">
                  {t(consent.documentType === "TERMS" ? "accepted" : "acknowledged")}{" "}
                  <span className="tabular">{dateFormat.format(new Date(consent.acceptedAt))}</span>
                </span>
                <span className="text-meta text-ink-3">
                  {t(consent.context === "REGISTRATION" ? "contextRegistration" : "contextUpdate")} ·{" "}
                  {consent.language.toUpperCase()}
                </span>
              </li>
            ))}
          </ul>
        )
      ) : legal.status === "error" ? (
        <FormMessage tone="error">{t("historyError")}</FormMessage>
      ) : (
        <div aria-busy="true" className="mt-3 h-20 bg-well" />
      )}
    </div>
  );
}

function DataExport() {
  const t = useTranslations("account.privacy");
  const tErrors = useTranslations("errors");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function download() {
    setPending(true);
    setError(null);
    try {
      const date = new Date().toISOString().slice(0, 10);
      await buyerApi.download("/me/export", `fabians-style-${date}.json`);
    } catch (e) {
      setError(e instanceof BuyerApiError && e.status === 429 ? tErrors("rateLimited", { seconds: e.retryAfter ?? 60 }) : tErrors("download"));
    } finally {
      setPending(false);
    }
  }

  return (
    <div>
      <h3 className="text-small font-medium">{t("exportTitle")}</h3>
      <p className="mt-1 max-w-[60ch] text-small text-ink-2">{t("exportLead")}</p>
      <button
        type="button"
        onClick={() => void download()}
        disabled={pending}
        className="mt-3 inline-flex items-center gap-2 text-small underline underline-offset-4 hover:no-underline disabled:cursor-wait"
      >
        <Download aria-hidden className="size-4" strokeWidth={1.5} />
        {pending ? t("exporting") : t("export")}
      </button>
      {error && (
        <div className="mt-3">
          <FormMessage tone="error">{error}</FormMessage>
        </div>
      )}
    </div>
  );
}

function DeleteAccount({ onDeleted }: { onDeleted: () => void }) {
  const t = useTranslations("account.privacy");
  const tAccount = useTranslations("account");
  const tErrors = useTranslations("errors");
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!confirmed) return;
    setPending(true);
    setError(null);
    try {
      await buyerApi.post("/me/delete", { currentPassword: password });
      onDeleted();
    } catch (e) {
      const problem = e instanceof BuyerApiError ? e.problem : undefined;
      setError(
        problem?.code === "identity.buyer.current-password-invalid"
          ? tAccount("page.password.currentInvalid")
          : e instanceof BuyerApiError && e.status === 429
            ? tErrors("rateLimited", { seconds: e.retryAfter ?? 60 })
            : tErrors("generic"),
      );
    } finally {
      setPending(false);
    }
  }

  return (
    <div>
      <h3 className="text-small font-medium">{t("deleteTitle")}</h3>
      <p className="mt-1 max-w-[60ch] text-small text-ink-2">{t("deleteLead")}</p>
      {!open ? (
        <button
          type="button"
          onClick={() => setOpen(true)}
          className="mt-3 text-small text-ink-2 underline underline-offset-4 hover:text-foreground"
        >
          {t("deleteOpen")}
        </button>
      ) : (
        <form onSubmit={submit} className="mt-4 grid gap-4 border border-edge p-4">
          <TextField
            label={tAccount("page.password.current")}
            type="password"
            autoComplete="current-password"
            required
            maxLength={1024}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <label className="flex items-start gap-3 text-small">
            <input
              type="checkbox"
              checked={confirmed}
              onChange={(e) => setConfirmed(e.target.checked)}
              className="mt-0.5 size-4 shrink-0 accent-foreground"
            />
            <span>{t("deleteConfirm")}</span>
          </label>
          {error && <FormMessage tone="error">{error}</FormMessage>}
          <div className="flex flex-wrap items-center gap-4">
            <SubmitButton pending={pending} disabled={pending || !confirmed || !password} className="sm:w-auto">
              {pending ? t("deleting") : t("deleteSubmit")}
            </SubmitButton>
            <button
              type="button"
              onClick={() => {
                setOpen(false);
                setPassword("");
                setConfirmed(false);
                setError(null);
              }}
              className="text-small text-ink-2 underline underline-offset-4 hover:text-foreground"
            >
              {t("deleteCancel")}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
