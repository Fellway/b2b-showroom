"use client";

import { useLocale, useTranslations } from "next-intl";
import { Download } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { Link, usePathname, useRouter } from "@/i18n/navigation";
import { BuyerApiError, buyerApi, useBuyerSession } from "@/lib/buyer/api";
import { buyerSession } from "@/lib/buyer/session";
import type { BuyerAccount, BuyerLineSheet, Page } from "@/lib/types";
import { routing } from "@/i18n/routing";
import { isSupportedCountry, splitPhone, toInternational } from "@/lib/phone";
import { FormMessage, SelectField, SubmitButton, TextField } from "./fields";
import { PhoneField } from "./phone-field";

const SHEETS_PAGE_SIZE = 10;

export function AccountPage() {
  const t = useTranslations("account");
  const state = useBuyerSession();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (state.status === "anonymous") router.replace({ pathname: "/account/login", query: { next: pathname } });
  }, [state.status, router, pathname]);

  // Dane konta z sesji pochodzą z chwili logowania (sprzed zapisania czasu logowania), więc ich `version` bywa
  // nieaktualna — przed edycją pobieramy bieżący stan, inaczej zapis kończy się konfliktem wersji.
  const authenticated = state.status === "authenticated";
  useEffect(() => {
    if (!authenticated) return;
    buyerApi
      .get<BuyerAccount>("/me")
      .then((fresh) => buyerSession.update(fresh))
      .catch(() => undefined);
  }, [authenticated]);

  if (state.status !== "authenticated") {
    return <div className="min-h-[60vh]" aria-busy="true" />;
  }
  const buyer = state.buyer;

  return (
    <div className="mx-auto w-full max-w-[1100px] px-5 pb-20 pt-10 md:px-8 md:pt-14">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-title font-medium md:text-[1.75rem] md:leading-[2.25rem]">{t("page.title")}</h1>
          <p className="mt-1 text-ink-2">{buyer.companyName}</p>
        </div>
        <button
          type="button"
          onClick={async () => {
            await buyerSession.logout();
            router.replace("/");
          }}
          className="text-small text-ink-2 underline underline-offset-4 hover:text-foreground"
        >
          {t("page.logout")}
        </button>
      </div>

      <div className="mt-10 grid gap-12 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] lg:gap-16">
        <MyLineSheets />
        <div className="flex flex-col gap-12">
          <CompanyDetails buyer={buyer} />
          <ProfileForm key={buyer.id} buyer={buyer} />
          <PasswordForm />
        </div>
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="border-t border-line pt-5">
      <h2 className="text-body font-medium">{title}</h2>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function CompanyDetails({ buyer }: { buyer: BuyerAccount }) {
  const t = useTranslations("account.page");
  return (
    <Section title={t("company")}>
      <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-small">
        <dt className="text-ink-3">{t("company")}</dt>
        <dd>{buyer.companyName}</dd>
        <dt className="text-ink-3">{t("taxId")}</dt>
        <dd className="tabular">
          {buyer.countryCode} {buyer.taxId}
        </dd>
        <dt className="text-ink-3">{t("email")}</dt>
        <dd className="break-all">{buyer.email}</dd>
      </dl>
      <p className="mt-3 text-meta text-ink-3">{t("companyHint")}</p>
    </Section>
  );
}

function ProfileForm({ buyer }: { buyer: BuyerAccount }) {
  const t = useTranslations("account.page");
  const tErrors = useTranslations("errors");
  const tLanguages = useTranslations("languages");
  const tAccount = useTranslations("account");
  const initialPhone = splitPhone(buyer.phone, isSupportedCountry(buyer.countryCode) ? buyer.countryCode : "PL");
  const [form, setForm] = useState({ contactName: buyer.contactName, phone: initialPhone.national, language: buyer.language });
  const [phoneCountry, setPhoneCountry] = useState(initialPhone.country);
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState<{ tone: "info" | "error"; text: string } | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setMessage(null);
    const phone = toInternational(phoneCountry, form.phone);
    if (!phone) {
      setMessage({ tone: "error", text: tAccount("register.errors.phone") });
      return;
    }
    setPending(true);
    try {
      await buyerApi.put("/me", { ...form, contactName: form.contactName.trim(), phone, version: buyer.version });
      buyerSession.update(await buyerApi.get<BuyerAccount>("/me"));
      setMessage({ tone: "info", text: t("saved") });
    } catch (error) {
      const code = error instanceof BuyerApiError ? error.problem.code : undefined;
      setMessage({
        tone: "error",
        text:
          code === "identity.buyer.concurrent-modification"
            ? t("concurrent")
            : code === "identity.buyer.phone"
              ? tErrors("validation")
              : tErrors("generic"),
      });
    } finally {
      setPending(false);
    }
  }

  return (
    <Section title={t("profile")}>
      <form onSubmit={submit} className="grid gap-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            label={t("contactName")}
            autoComplete="name"
            required
            maxLength={120}
            value={form.contactName}
            onChange={(e) => setForm((f) => ({ ...f, contactName: e.target.value }))}
          />
          <PhoneField
            label={t("phone")}
            required
            country={phoneCountry}
            onCountryChange={setPhoneCountry}
            national={form.phone}
            onNationalChange={(value) => setForm((f) => ({ ...f, phone: value }))}
          />
        </div>
        <SelectField label={t("language")} value={form.language} onChange={(e) => setForm((f) => ({ ...f, language: e.target.value }))}>
          {routing.locales.map((code) => (
            <option key={code} value={code}>
              {tLanguages(code)}
            </option>
          ))}
        </SelectField>
        {message && <FormMessage tone={message.tone}>{message.text}</FormMessage>}
        <SubmitButton pending={pending} className="sm:w-auto sm:justify-self-start">
          {pending ? t("saving") : t("save")}
        </SubmitButton>
      </form>
    </Section>
  );
}

function PasswordForm() {
  const t = useTranslations("account");
  const tErrors = useTranslations("errors");
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState<{ tone: "info" | "error"; text: string } | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setMessage(null);
    const length = [...next].length;
    if (length < 12 || length > 128) {
      setMessage({ tone: "error", text: t("register.errors.password") });
      return;
    }
    setPending(true);
    try {
      buyerSession.adopt(await buyerApi.post<unknown>("/me/password", { currentPassword: current, newPassword: next }));
      setCurrent("");
      setNext("");
      setMessage({ tone: "info", text: t("page.password.changed") });
    } catch (error) {
      const problem = error instanceof BuyerApiError ? error.problem : undefined;
      setMessage({
        tone: "error",
        text:
          problem?.code === "identity.buyer.current-password-invalid"
            ? t("page.password.currentInvalid")
            : problem?.code === "identity.password.policy"
              ? t("register.errors.password")
              : error instanceof BuyerApiError && error.status === 429
                ? tErrors("rateLimited", { seconds: error.retryAfter ?? 60 })
                : tErrors("generic"),
      });
    } finally {
      setPending(false);
    }
  }

  return (
    <Section title={t("page.password.title")}>
      <form onSubmit={submit} className="grid gap-4">
        {/* ukryte pole z loginem — menedżery haseł wiedzą, do którego konta należy nowe hasło */}
        <input type="text" autoComplete="username" hidden readOnly value={buyerEmail()} />
        <TextField label={t("page.password.current")} type="password" autoComplete="current-password" required maxLength={1024} value={current} onChange={(e) => setCurrent(e.target.value)} />
        <TextField label={t("page.password.new")} type="password" autoComplete="new-password" required maxLength={128} value={next} onChange={(e) => setNext(e.target.value)} hint={t("passwordHint")} />
        {message && <FormMessage tone={message.tone}>{message.text}</FormMessage>}
        <SubmitButton pending={pending} className="sm:w-auto sm:justify-self-start">
          {t("page.password.submit")}
        </SubmitButton>
      </form>
    </Section>
  );
}

function buyerEmail() {
  const state = buyerSession.snapshot();
  return state.status === "authenticated" ? state.buyer.email : "";
}

function MyLineSheets() {
  const t = useTranslations("account.page.sheets");
  const tAccount = useTranslations("account");
  const tErrors = useTranslations("errors");
  const locale = useLocale();
  const [pages, setPages] = useState<Page<BuyerLineSheet>[]>([]);
  const [failed, setFailed] = useState(false);
  const [downloading, setDownloading] = useState<string | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);

  const fetchPage = (page: number) =>
    buyerApi.get<Page<BuyerLineSheet>>(`/line-sheets?page=${page}&size=${SHEETS_PAGE_SIZE}`);

  useEffect(() => {
    let cancelled = false;
    fetchPage(0)
      .then((first) => !cancelled && setPages([first]))
      .catch(() => !cancelled && setFailed(true));
    return () => {
      cancelled = true;
    };
  }, []);

  async function loadMore(page: number) {
    try {
      const result = await fetchPage(page);
      setPages((current) => [...current.filter((p) => p.page !== page), result].sort((a, b) => a.page - b.page));
    } catch {
      setFailed(true);
    }
  }

  const items = pages.flatMap((p) => p.items);
  const last = pages.at(-1);
  const dateFormat = new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short" });

  async function download(sheet: BuyerLineSheet) {
    setDownloadError(null);
    setDownloading(sheet.publicId);
    try {
      await buyerApi.download(`/line-sheets/${sheet.publicId}/pdf`, `${sheet.referenceNumber}.pdf`);
    } catch {
      setDownloadError(tErrors("download"));
    } finally {
      setDownloading(null);
    }
  }

  return (
    <Section title={t("title")}>
      {failed ? (
        <FormMessage tone="error">{tErrors("generic")}</FormMessage>
      ) : !last ? (
        <div aria-busy="true" className="flex flex-col">
          {[0, 1, 2].map((i) => (
            <div key={i} className="h-14 border-b border-line" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="flex flex-col items-start gap-3">
          <p className="max-w-[46ch] text-ink-2">{t("empty")}</p>
          <Link href="/" className="text-small underline underline-offset-4 hover:no-underline">
            {tAccount("backToCatalog")}
          </Link>
        </div>
      ) : (
        <>
          <ul className="divide-y divide-line border-y border-line">
            {items.map((sheet) => (
              <li key={sheet.id} className="flex items-center gap-4 py-3">
                <div className="min-w-0 flex-1">
                  <p className="tabular text-small font-medium">{sheet.referenceNumber}</p>
                  <p className="tabular text-meta text-ink-3">
                    {dateFormat.format(new Date(sheet.createdAt))} · {t("items", { count: sheet.itemCount })}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => download(sheet)}
                  disabled={downloading !== null}
                  className="inline-flex shrink-0 items-center gap-2 text-small text-ink-2 hover:text-foreground disabled:cursor-wait"
                >
                  <Download aria-hidden className="size-4" strokeWidth={1.5} />
                  {t("download")}
                </button>
              </li>
            ))}
          </ul>
          {downloadError && (
            <div className="mt-3">
              <FormMessage tone="error">{downloadError}</FormMessage>
            </div>
          )}
          {last.page + 1 < last.totalPages && (
            <button
              type="button"
              onClick={() => void loadMore(last.page + 1)}
              className="mt-4 text-small underline underline-offset-4 hover:no-underline"
            >
              {t("more")}
            </button>
          )}
        </>
      )}
    </Section>
  );
}
