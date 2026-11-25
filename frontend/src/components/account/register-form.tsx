"use client";

import { useSearchParams } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { useMemo, useState, type FormEvent } from "react";
import { Link } from "@/i18n/navigation";
import { readProblem } from "@/lib/problem";
import { buyerAuthPost } from "@/lib/buyer/session";
import { isSupportedCountry, toInternational } from "@/lib/phone";
import type { CountryCode } from "libphonenumber-js/min";
import { AccountColumn, CheckField, FormMessage, SelectField, SubmitButton, TextField, safeNext } from "./fields";
import { PhoneField } from "./phone-field";

/** Rynki marki na początku listy, potem pozostałe kraje Europy (kolejność alfabetyczna w języku strony). */
const PRIMARY_COUNTRIES = ["PL", "CZ", "SK", "UA"];
const OTHER_COUNTRIES = [
  "AT", "BE", "BG", "CH", "CY", "DE", "DK", "EE", "ES", "FI", "FR", "GB", "GR", "HR", "HU", "IE", "IT", "LT", "LU",
  "LV", "MD", "MT", "NL", "NO", "PT", "RO", "SE", "SI",
];
const DEFAULT_COUNTRY: Record<string, string> = { pl: "PL", cs: "CZ", uk: "UA" };
const COMPANY_COUNTRIES = new Set([...PRIMARY_COUNTRIES, ...OTHER_COUNTRIES]);
const PRIVACY_POLICY_URL = process.env.NEXT_PUBLIC_PRIVACY_POLICY_URL;
const PASSWORD_MIN = 12;
const PASSWORD_MAX = 128;

type Form = {
  companyName: string;
  countryCode: string;
  taxId: string;
  contactName: string;
  phone: string;
  email: string;
  password: string;
  termsAccepted: boolean;
  privacyAccepted: boolean;
};

type FieldKey = "companyName" | "countryCode" | "taxId" | "contactName" | "phone" | "email" | "password" | "consent";

/** Kody błędów backendu → pole formularza i komunikat (namespace `account.register.errors`). */
const CODE_TO_FIELD: Record<string, [FieldKey, string]> = {
  "identity.buyer.tax-id": ["taxId", "taxId"],
  "identity.buyer.country": ["countryCode", "country"],
  "identity.buyer.phone": ["phone", "phone"],
  "identity.buyer.company-name": ["companyName", "required"],
  "identity.buyer.contact-name": ["contactName", "required"],
  "identity.email": ["email", "email"],
  "identity.password.policy": ["password", "password"],
  "identity.buyer.consent-required": ["consent", "consent"],
};

/** @param suggestedCountry kraj z żądania (nagłówek proxy / Accept-Language) — tylko podpowiedź, do zmiany */
export function RegisterForm({ suggestedCountry }: { suggestedCountry?: string | null }) {
  const t = useTranslations("account");
  const tErrors = useTranslations("errors");
  const locale = useLocale();
  const searchParams = useSearchParams();
  const initialCompanyCountry =
    suggestedCountry && COMPANY_COUNTRIES.has(suggestedCountry) ? suggestedCountry : (DEFAULT_COUNTRY[locale] ?? "");
  const [phoneCountry, setPhoneCountry] = useState<CountryCode>(
    isSupportedCountry(suggestedCountry) ? suggestedCountry : isSupportedCountry(initialCompanyCountry) ? initialCompanyCountry : "PL",
  );
  // numer kierunkowy podąża za krajem firmy, dopóki kupiec nie wybierze go sam
  const [phoneCountryTouched, setPhoneCountryTouched] = useState(false);
  const [form, setForm] = useState<Form>({
    companyName: "",
    countryCode: initialCompanyCountry,
    taxId: "",
    contactName: "",
    phone: "",
    email: "",
    password: "",
    termsAccepted: false,
    privacyAccepted: false,
  });
  const [errors, setErrors] = useState<Partial<Record<FieldKey, string>>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [sentTo, setSentTo] = useState<string | null>(null);
  const [resent, setResent] = useState(false);

  const countryNames = useMemo(() => {
    const names = new Intl.DisplayNames([locale], { type: "region" });
    const name = (code: string) => names.of(code) ?? code;
    const collator = new Intl.Collator(locale);
    return {
      primary: PRIMARY_COUNTRIES.map((code) => ({ code, name: name(code) })),
      other: OTHER_COUNTRIES.map((code) => ({ code, name: name(code) })).sort((a, b) => collator.compare(a.name, b.name)),
    };
  }, [locale]);

  const set = <K extends keyof Form>(key: K, value: Form[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
    const field: FieldKey = key === "termsAccepted" || key === "privacyAccepted" ? "consent" : key;
    setErrors((current) => ({ ...current, [field]: undefined }));
  };

  function validate(): Partial<Record<FieldKey, string>> {
    const found: Partial<Record<FieldKey, string>> = {};
    const required = t("register.errors.required");
    if (!form.companyName.trim()) found.companyName = required;
    if (!form.countryCode) found.countryCode = t("register.errors.country");
    if (!form.taxId.trim()) found.taxId = required;
    if (!form.contactName.trim()) found.contactName = required;
    if (!toInternational(phoneCountry, form.phone)) found.phone = t("register.errors.phone");
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/.test(form.email.trim())) found.email = t("register.errors.email");
    const length = [...form.password].length;
    if (length < PASSWORD_MIN || length > PASSWORD_MAX) found.password = t("register.errors.password");
    if (!form.termsAccepted || !form.privacyAccepted) found.consent = t("register.errors.consent");
    return found;
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    const found = validate();
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    setPending(true);
    const body = {
      ...form,
      companyName: form.companyName.trim(),
      taxId: form.taxId.trim(),
      contactName: form.contactName.trim(),
      // zawsze z numerem kierunkowym, np. „+48 600 111 222”
      phone: toInternational(phoneCountry, form.phone) ?? form.phone.trim(),
      email: form.email.trim(),
    };
    try {
      const response = await buyerAuthPost("/register", body, locale);
      if (response.ok) {
        setSentTo(body.email);
        return;
      }
      if (response.status === 429) {
        const seconds = Number.parseInt(response.headers.get("Retry-After") ?? "60", 10);
        setFormError(tErrors("rateLimited", { seconds: Number.isFinite(seconds) ? seconds : 60 }));
        return;
      }
      const problem = await readProblem(response);
      const mapped = problem.code ? CODE_TO_FIELD[problem.code] : undefined;
      if (mapped) setErrors({ [mapped[0]]: t(`register.errors.${mapped[1]}`) });
      else setFormError(problem.code === "request.invalid" ? tErrors("validation") : tErrors("generic"));
    } catch {
      setFormError(tErrors("network"));
    } finally {
      setPending(false);
    }
  }

  if (sentTo) {
    return (
      <AccountColumn title={t("register.doneTitle")} lead={t("register.doneLead", { email: sentTo })}>
        <p className="text-small text-ink-3">{t("register.doneHint")}</p>
        {resent ? (
          <p aria-live="polite" className="mt-4 text-small text-ink-2">
            {t("login.resent")}
          </p>
        ) : (
          <button
            type="button"
            onClick={async () => {
              await buyerAuthPost("/resend-verification", { email: sentTo }, locale).catch(() => undefined);
              setResent(true);
            }}
            className="mt-4 text-small underline underline-offset-4 hover:no-underline"
          >
            {t("login.resend")}
          </button>
        )}
        <div className="mt-8 border-t border-line pt-6">
          <Link href="/" className="text-small underline underline-offset-4 hover:no-underline">
            {t("backToCatalog")}
          </Link>
        </div>
      </AccountColumn>
    );
  }

  const next = searchParams.get("next");
  return (
    <AccountColumn title={t("register.title")} lead={t("register.lead")} wide>
      <form onSubmit={submit} noValidate className="grid gap-8">
        <fieldset className="grid gap-4">
          <legend className="mb-4 text-small font-medium">{t("register.company")}</legend>
          <TextField
            label={t("register.companyName")}
            autoComplete="organization"
            maxLength={200}
            value={form.companyName}
            onChange={(e) => set("companyName", e.target.value)}
            error={errors.companyName}
          />
          <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
            <SelectField
              label={t("register.country")}
              autoComplete="country"
              value={form.countryCode}
              onChange={(e) => {
                set("countryCode", e.target.value);
                if (!phoneCountryTouched && isSupportedCountry(e.target.value)) setPhoneCountry(e.target.value);
              }}
              error={errors.countryCode}
            >
              <option value="" disabled>
                {t("register.countryPlaceholder")}
              </option>
              {countryNames.primary.map((c) => (
                <option key={c.code} value={c.code}>
                  {c.name}
                </option>
              ))}
              <option disabled>──────────</option>
              {countryNames.other.map((c) => (
                <option key={c.code} value={c.code}>
                  {c.name}
                </option>
              ))}
            </SelectField>
            <TextField
              label={t("register.taxId")}
              maxLength={40}
              value={form.taxId}
              onChange={(e) => set("taxId", e.target.value)}
              hint={form.countryCode === "PL" ? t("register.taxIdHint") : undefined}
              error={errors.taxId}
            />
          </div>
        </fieldset>

        <fieldset className="grid gap-4 border-t border-line pt-6">
          <legend className="float-left mb-4 text-small font-medium">{t("register.contact")}</legend>
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField
              label={t("register.contactName")}
              autoComplete="name"
              maxLength={120}
              value={form.contactName}
              onChange={(e) => set("contactName", e.target.value)}
              error={errors.contactName}
            />
            <PhoneField
              label={t("register.phone")}
              country={phoneCountry}
              onCountryChange={(next) => {
                setPhoneCountry(next);
                setPhoneCountryTouched(true);
                setErrors((current) => ({ ...current, phone: undefined }));
              }}
              national={form.phone}
              onNationalChange={(value) => set("phone", value)}
              error={errors.phone}
            />
          </div>
        </fieldset>

        <fieldset className="grid gap-4 border-t border-line pt-6">
          <legend className="float-left mb-4 text-small font-medium">{t("register.access")}</legend>
          <TextField
            label={t("email")}
            type="email"
            autoComplete="email"
            maxLength={254}
            value={form.email}
            onChange={(e) => set("email", e.target.value)}
            error={errors.email}
          />
          <TextField
            label={t("password")}
            type="password"
            autoComplete="new-password"
            maxLength={PASSWORD_MAX}
            value={form.password}
            onChange={(e) => set("password", e.target.value)}
            hint={t("passwordHint")}
            error={errors.password}
          />
        </fieldset>

        <div className="grid gap-3 border-t border-line pt-6">
          <CheckField
            label={t("register.terms")}
            checked={form.termsAccepted}
            onChange={(e) => set("termsAccepted", e.target.checked)}
            invalid={Boolean(errors.consent) && !form.termsAccepted}
          />
          <CheckField
            label={t.rich("register.privacy", {
              policy: (chunks) =>
                PRIVACY_POLICY_URL ? (
                  <a href={PRIVACY_POLICY_URL} target="_blank" rel="noopener" className="underline underline-offset-4 hover:text-foreground">
                    {chunks}
                  </a>
                ) : null,
            })}
            checked={form.privacyAccepted}
            onChange={(e) => set("privacyAccepted", e.target.checked)}
            invalid={Boolean(errors.consent) && !form.privacyAccepted}
          />
          {errors.consent && <FormMessage tone="error">{errors.consent}</FormMessage>}
        </div>

        <div className="grid gap-3">
          {formError && <FormMessage tone="error">{formError}</FormMessage>}
          <SubmitButton pending={pending}>{pending ? t("register.submitting") : t("register.submit")}</SubmitButton>
          <p className="text-small text-ink-2">
            {t("register.haveAccount")}{" "}
            <Link
              href={{ pathname: "/account/login", query: next ? { next: safeNext(next) } : undefined }}
              className="text-foreground underline underline-offset-4 hover:no-underline"
            >
              {t("register.signIn")}
            </Link>
          </p>
        </div>
      </form>
    </AccountColumn>
  );
}
