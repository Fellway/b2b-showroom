"use client";

import Image from "next/image";
import { useLocale, useTranslations } from "next-intl";
import { ArrowLeft, Download, Minus, Plus, X } from "lucide-react";
import { useId, useState, type FormEvent } from "react";
import { Sheet, SheetContent, SheetDescription, SheetTitle } from "@/components/ui/sheet";
import { Link } from "@/i18n/navigation";
import { formatMoney } from "@/lib/format";
import { problemMessageKey, readProblem } from "@/lib/problem";
import type { CreatedLineSheet, SalesRepresentative } from "@/lib/types";
import { useHydrated } from "@/lib/use-hydrated";
import { cn } from "@/lib/utils";
import { groupLines, summarize, useSelection, type SelectionLine } from "@/store/selection";
import { RepresentativeContact } from "@/components/site/representative-contact";
import { track, visitorId } from "@/lib/analytics";

type Step = "lines" | "form" | "done";

type Contact = { companyName: string; contactName: string; email: string; phone: string; taxId: string; message: string };

const EMPTY_CONTACT: Contact = { companyName: "", contactName: "", email: "", phone: "", taxId: "", message: "" };

export function SelectionDrawer({ representatives }: { representatives: SalesRepresentative[] }) {
  const t = useTranslations("selection");
  const open = useSelection((s) => s.drawerOpen);
  const setOpen = useSelection((s) => s.setDrawerOpen);
  const hydrated = useHydrated();
  const lines = useSelection((s) => s.lines);
  const [step, setStep] = useState<Step>("lines");
  const [created, setCreated] = useState<CreatedLineSheet | null>(null);

  const visibleLines = hydrated ? lines : [];

  function onOpenChange(next: boolean) {
    setOpen(next);
    if (!next && step !== "done") setStep("lines");
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="right"
        closeLabel={t("close")}
        className="w-full gap-0 border-line p-0 shadow-panel data-[side=right]:w-full data-[side=right]:sm:max-w-[480px]"
      >
        <div className="flex h-(--header-height) shrink-0 items-center border-b border-line px-6">
          <SheetTitle className="text-body font-medium">
            {t("title")}
            {visibleLines.length > 0 && step !== "done" && (
              <span className="tabular ml-2 font-normal text-ink-3">{visibleLines.length}</span>
            )}
          </SheetTitle>
          <SheetDescription className="sr-only">{t("empty")}</SheetDescription>
        </div>

        {step === "done" && created ? (
          <DoneStep
            created={created}
            representatives={representatives}
            onRestart={() => {
              useSelection.getState().clear();
              setCreated(null);
              setStep("lines");
            }}
            onClose={() => onOpenChange(false)}
          />
        ) : visibleLines.length === 0 ? (
          <EmptyStep onClose={() => onOpenChange(false)} />
        ) : step === "form" ? (
          <FormStep
            lines={visibleLines}
            onBack={() => setStep("lines")}
            onCreated={(result) => {
              setCreated(result);
              setStep("done");
            }}
          />
        ) : (
          <LinesStep lines={visibleLines} onContinue={() => setStep("form")} />
        )}
      </SheetContent>
    </Sheet>
  );
}

function EmptyStep({ onClose }: { onClose: () => void }) {
  const t = useTranslations("selection");
  return (
    <div className="flex flex-1 flex-col items-start gap-5 px-6 py-10">
      <p className="max-w-[40ch] text-ink-2">{t("empty")}</p>
      <Link href="/" onClick={onClose} className="underline underline-offset-4 hover:no-underline">
        {t("browse")}
      </Link>
    </div>
  );
}

function LinesStep({ lines, onContinue }: { lines: SelectionLine[]; onContinue: () => void }) {
  const t = useTranslations("selection");
  const locale = useLocale();
  const setQuantity = useSelection((s) => s.setQuantity);
  const remove = useSelection((s) => s.remove);
  const removeGroup = useSelection((s) => s.removeGroup);
  const setOpen = useSelection((s) => s.setDrawerOpen);
  const groups = groupLines(lines);
  const totals = summarize(lines);

  const removeTracked = (line: SelectionLine) => {
    track({ type: "SELECTION_REMOVED", language: locale, productId: line.productId, variantId: line.variantId });
    remove(line.variantId);
  };

  return (
    <>
      <ul className="flex-1 divide-y divide-line overflow-y-auto">
        {groups.map((group) => (
          <li key={group.key} className="flex gap-4 px-6 py-5">
            <Link
              href={`/p/${group.slug}`}
              onClick={() => setOpen(false)}
              className="relative aspect-[2/3] w-16 shrink-0 self-start bg-well"
            >
              {group.imageUrl && <Image src={group.imageUrl} alt="" fill sizes="64px" className="object-cover" />}
            </Link>
            <div className="min-w-0 flex-1">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-small font-medium">{group.name}</p>
                  <p className="mt-0.5 flex items-center gap-1.5 text-meta text-ink-3">
                    <span
                      aria-hidden
                      className="inline-block size-2.5 border border-black/15"
                      style={{ backgroundColor: group.colorHex ?? "transparent" }}
                    />
                    {group.colorName} · {group.styleCode}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    group.lines.forEach((line) =>
                      track({ type: "SELECTION_REMOVED", language: locale, productId: line.productId, variantId: line.variantId }),
                    );
                    removeGroup(group.productId, group.colorId);
                  }}
                  className="shrink-0 text-meta text-ink-3 underline underline-offset-4 hover:text-foreground"
                >
                  {t("removeGroup")}
                </button>
              </div>
              <ul className="mt-3 flex flex-col gap-1.5">
                {group.lines.map((line) => (
                  <li key={line.variantId} className="flex items-center gap-3">
                    <span className="tabular w-9 text-small">{line.size}</span>
                    <MiniStepper
                      value={line.quantity}
                      label={line.size}
                      onChange={(q) => setQuantity(line.variantId, q)}
                    />
                    <button
                      type="button"
                      aria-label={t("remove", { name: group.name, color: group.colorName, size: line.size })}
                      onClick={() => removeTracked(line)}
                      className="ml-auto flex size-8 items-center justify-center text-ink-3 hover:text-foreground"
                    >
                      <X aria-hidden className="size-4" strokeWidth={1.5} />
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          </li>
        ))}
      </ul>

      <div className="shrink-0 border-t border-line px-6 pb-6 pt-4">
        <dl className="tabular grid grid-cols-[1fr_auto] gap-y-1 text-small">
          <dt className="text-ink-3">{t("summary.lines")}</dt>
          <dd className="text-right">{totals.lines}</dd>
          {totals.hasQuantities && (
            <>
              <dt className="text-ink-3">{t("summary.units")}</dt>
              <dd className="text-right">{totals.units}</dd>
              <dt className="text-ink-3">{t("summary.value")}</dt>
              <dd className="text-right font-medium">{formatMoney(totals.value, totals.currency, locale)}</dd>
            </>
          )}
        </dl>
        {totals.partial && <p className="mt-2 text-meta text-ink-3">{t("summary.partial")}</p>}
        <button
          type="button"
          onClick={onContinue}
          className="mt-4 flex h-12 w-full items-center justify-center bg-foreground px-6 text-small font-medium text-background transition-colors hover:bg-ink-2"
        >
          {t("continue")}
        </button>
      </div>
    </>
  );
}

function FormStep({
  lines,
  onBack,
  onCreated,
}: {
  lines: SelectionLine[];
  onBack: () => void;
  onCreated: (result: CreatedLineSheet) => void;
}) {
  const t = useTranslations("selection");
  const tErrors = useTranslations("errors");
  const locale = useLocale();
  const [contact, setContact] = useState<Contact>(EMPTY_CONTACT);
  const [consent, setConsent] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [consentError, setConsentError] = useState(false);
  const consentId = useId();

  const hasData = Object.values(contact).some((value) => value.trim() !== "");
  const field = (key: keyof Contact) => ({
    value: contact[key],
    onChange: (e: { target: { value: string } }) => setContact((c) => ({ ...c, [key]: e.target.value })),
  });

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (hasData && !consent) {
      setConsentError(true);
      return;
    }
    setSubmitting(true);
    const trimmed = Object.fromEntries(
      Object.entries(contact).map(([key, value]) => [key, value.trim() === "" ? null : value.trim()]),
    );
    try {
      const response = await fetch(`/api/v1/line-sheets?lang=${locale}`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({
          items: lines.map((line) => ({ variantId: line.variantId, quantity: line.quantity })),
          ...trimmed,
          privacyAccepted: hasData && consent,
          visitorId: visitorId() ?? null,
        }),
      });
      if (response.ok) {
        onCreated((await response.json()) as CreatedLineSheet);
        return;
      }
      if (response.status === 429) {
        const seconds = Number.parseInt(response.headers.get("Retry-After") ?? "60", 10);
        setError(tErrors("rateLimited", { seconds: Number.isFinite(seconds) ? seconds : 60 }));
        return;
      }
      const { key } = problemMessageKey(await readProblem(response));
      setError(tErrors(key));
    } catch {
      setError(tErrors("network"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate={false}>
      <div className="flex-1 overflow-y-auto px-6 py-6">
        <button
          type="button"
          onClick={onBack}
          className="inline-flex items-center gap-1.5 text-small text-ink-3 hover:text-foreground"
        >
          <ArrowLeft aria-hidden className="size-4" strokeWidth={1.5} />
          {t("form.back")}
        </button>
        <h3 className="mt-5 text-body font-medium">{t("form.title")}</h3>
        <p className="mt-1 text-small text-ink-2">{t("form.lead")}</p>

        <div className="mt-6 grid gap-4">
          <TextField label={t("form.company")} autoComplete="organization" maxLength={200} {...field("companyName")} />
          <TextField label={t("form.contactName")} autoComplete="name" maxLength={120} {...field("contactName")} />
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField label={t("form.email")} type="email" autoComplete="email" maxLength={254} {...field("email")} />
            <TextField label={t("form.phone")} type="tel" autoComplete="tel" maxLength={32} {...field("phone")} />
          </div>
          <TextField label={t("form.taxId")} maxLength={32} {...field("taxId")} />
          <label className="grid gap-1.5">
            <span className="text-meta text-ink-3">{t("form.message")}</span>
            <textarea
              rows={3}
              maxLength={2000}
              {...field("message")}
              className="resize-y border border-edge bg-transparent px-3 py-2 text-small outline-none focus-visible:border-foreground focus-visible:outline-none"
            />
          </label>
          {hasData && (
            <div className="grid gap-1.5">
              <label htmlFor={consentId} className="flex cursor-pointer items-start gap-3 text-meta text-ink-2">
                <input
                  id={consentId}
                  type="checkbox"
                  checked={consent}
                  onChange={(e) => {
                    setConsent(e.target.checked);
                    setConsentError(false);
                  }}
                  aria-invalid={consentError}
                  className="mt-0.5 size-4 shrink-0 accent-foreground"
                />
                {t("form.consent")}
              </label>
              {consentError && (
                <p className="text-meta text-danger" role="alert">
                  {t("form.consentRequired")}
                </p>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="shrink-0 border-t border-line px-6 pb-6 pt-4">
        {error && (
          <p className="mb-3 text-small text-danger" role="alert">
            {error}
          </p>
        )}
        <button
          type="submit"
          disabled={submitting}
          className="flex h-12 w-full items-center justify-center bg-foreground px-6 text-small font-medium text-background transition-colors hover:bg-ink-2 disabled:cursor-wait disabled:bg-ink-2"
        >
          {submitting ? t("form.submitting") : t("form.submit")}
        </button>
      </div>
    </form>
  );
}

function DoneStep({
  created,
  representatives,
  onRestart,
  onClose,
}: {
  created: CreatedLineSheet;
  representatives: SalesRepresentative[];
  onRestart: () => void;
  onClose: () => void;
}) {
  const t = useTranslations("selection");
  return (
    <div className="flex flex-1 flex-col overflow-y-auto px-6 py-8">
      <h3 className="tabular text-title font-medium text-balance">
        {t.rich("done.title", {
          reference: created.referenceNumber,
          ref: (chunks) => <span className="whitespace-nowrap">{chunks}</span>,
        })}
      </h3>
      <p className="mt-3 text-small text-ink-2">{t("done.lead")}</p>
      <a
        href={created.pdfUrl}
        download
        className="mt-6 flex h-12 items-center justify-center gap-2 bg-foreground px-6 text-small font-medium text-background transition-colors hover:bg-ink-2"
      >
        <Download aria-hidden className="size-4" strokeWidth={1.5} />
        {t("done.download")}
      </a>
      {representatives.length > 0 && (
        <div className="mt-8 flex flex-col gap-5 border-t border-line pt-6">
          {representatives.map((rep) => (
            <RepresentativeContact key={rep.id} representative={rep} />
          ))}
        </div>
      )}
      <div className="mt-auto flex flex-wrap gap-x-6 gap-y-2 pt-10 text-small">
        <button type="button" onClick={onClose} className="underline underline-offset-4 hover:no-underline">
          {t("done.keep")}
        </button>
        <button type="button" onClick={onRestart} className="text-ink-3 underline underline-offset-4 hover:text-foreground">
          {t("done.newSheet")}
        </button>
      </div>
    </div>
  );
}

function TextField({ label, className, ...props }: React.ComponentProps<"input"> & { label: string }) {
  return (
    <label className="grid gap-1.5">
      <span className="text-meta text-ink-3">{label}</span>
      <input
        {...props}
        className={cn(
          "h-11 border border-edge bg-transparent px-3 text-small outline-none focus-visible:border-foreground focus-visible:outline-none user-invalid:border-danger",
          className,
        )}
      />
    </label>
  );
}

function MiniStepper({
  value,
  label,
  onChange,
}: {
  value: number | null;
  label: string;
  onChange: (value: number | null) => void;
}) {
  const t = useTranslations("selection");
  const current = value ?? 0;
  const set = (next: number) => onChange(next <= 0 ? null : Math.min(next, 9999));
  return (
    <div className="flex items-center border border-edge">
      <button
        type="button"
        aria-label={t("decrease", { size: label })}
        onClick={() => set(current - 1)}
        disabled={current === 0}
        className="flex size-8 items-center justify-center text-ink-2 hover:text-foreground disabled:text-line"
      >
        <Minus aria-hidden className="size-3.5" strokeWidth={1.5} />
      </button>
      <input
        type="number"
        inputMode="numeric"
        min={0}
        max={9999}
        aria-label={`${t("quantity")} ${label}`}
        value={value ?? ""}
        placeholder=""
        onChange={(e) => {
          const parsed = Number.parseInt(e.target.value, 10);
          onChange(Number.isFinite(parsed) && parsed > 0 ? Math.min(parsed, 9999) : null);
        }}
        className="tabular h-8 w-11 border-x border-line bg-transparent text-center text-small outline-none [appearance:textfield] focus-visible:bg-well [&::-webkit-inner-spin-button]:appearance-none"
      />
      <button
        type="button"
        aria-label={t("increase", { size: label })}
        onClick={() => set(current + 1)}
        className="flex size-8 items-center justify-center text-ink-2 hover:text-foreground"
      >
        <Plus aria-hidden className="size-3.5" strokeWidth={1.5} />
      </button>
    </div>
  );
}
