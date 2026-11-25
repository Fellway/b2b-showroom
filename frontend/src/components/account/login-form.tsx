"use client";

import { useSearchParams } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useState, type FormEvent } from "react";
import { Link, useRouter } from "@/i18n/navigation";
import { useBuyerSession } from "@/lib/buyer/api";
import { buyerAuthPost, buyerSession } from "@/lib/buyer/session";
import type { BuyerStatus } from "@/lib/types";
import { useSelection } from "@/store/selection";
import { AccountColumn, FormMessage, SubmitButton, TextField, safeNext } from "./fields";

export function LoginForm() {
  const t = useTranslations("account");
  const tErrors = useTranslations("errors");
  const locale = useLocale();
  const router = useRouter();
  const searchParams = useSearchParams();
  const state = useBuyerSession();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [inactive, setInactive] = useState<BuyerStatus | null>(null);
  const [resent, setResent] = useState(false);

  const next = safeNext(searchParams.get("next"));
  const reopenSelection = searchParams.get("selection") === "1";

  useEffect(() => {
    if (state.status !== "authenticated") return;
    if (reopenSelection) useSelection.getState().setDrawerOpen(true);
    router.replace(next);
  }, [state.status, router, next, reopenSelection]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setInactive(null);
    setResent(false);
    setPending(true);
    const result = await buyerSession.login(email.trim(), password);
    setPending(false);
    if (result.ok) return;
    switch (result.reason) {
      case "credentials":
        setError(t("login.invalid"));
        break;
      case "inactive":
        setInactive(result.accountStatus);
        break;
      case "rate-limited":
        setError(tErrors("rateLimited", { seconds: result.retryAfter }));
        break;
      case "network":
        setError(tErrors("network"));
        break;
      default:
        setError(tErrors("generic"));
    }
  }

  async function resend() {
    await buyerAuthPost("/resend-verification", { email: email.trim() }, locale).catch(() => undefined);
    setResent(true);
  }

  return (
    <AccountColumn title={t("login.title")} lead={t("login.lead")}>
      <form onSubmit={submit} className="grid gap-5">
        <TextField
          label={t("email")}
          type="email"
          autoComplete="username"
          required
          maxLength={254}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <TextField
          label={t("password")}
          type="password"
          autoComplete="current-password"
          required
          maxLength={1024}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {error && <FormMessage tone="error">{error}</FormMessage>}
        {inactive && (
          <FormMessage tone="info">
            {t(`login.status.${inactive}`)}
            {inactive === "PENDING_EMAIL" && !resent && (
              <>
                {" "}
                <button type="button" onClick={resend} className="text-foreground underline underline-offset-4 hover:no-underline">
                  {t("login.resend")}
                </button>
              </>
            )}
            {resent && <span className="mt-1 block">{t("login.resent")}</span>}
          </FormMessage>
        )}
        <SubmitButton pending={pending}>{pending ? t("login.submitting") : t("login.submit")}</SubmitButton>
      </form>
      <div className="mt-6 flex flex-col gap-3 border-t border-line pt-6 text-small">
        <Link href="/account/reset-password" className="self-start underline underline-offset-4 hover:no-underline">
          {t("login.forgot")}
        </Link>
        <p className="text-ink-2">
          {t("login.noAccount")}{" "}
          <Link
            href={{ pathname: "/account/register", query: searchParams.get("next") ? { next } : undefined }}
            className="text-foreground underline underline-offset-4 hover:no-underline"
          >
            {t("login.register")}
          </Link>
        </p>
      </div>
    </AccountColumn>
  );
}
