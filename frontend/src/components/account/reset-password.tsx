"use client";

import { useLocale, useTranslations } from "next-intl";
import { useState, type FormEvent } from "react";
import { Link } from "@/i18n/navigation";
import { readProblem } from "@/lib/problem";
import { buyerAuthPost } from "@/lib/buyer/session";
import { useHydrated } from "@/lib/use-hydrated";
import { AccountColumn, FormMessage, SubmitButton, TextField } from "./fields";
import { useTokenFromLink } from "./token-link";

/** Bez tokenu: prośba o link. Z tokenem z e-maila: ustawienie nowego hasła. */
export function ResetPassword() {
  const hydrated = useHydrated();
  const token = useTokenFromLink();
  if (!hydrated) return null;
  return token ? <NewPasswordForm token={token} /> : <RequestForm />;
}

function RequestForm() {
  const t = useTranslations("account");
  const tErrors = useTranslations("errors");
  const locale = useLocale();
  const [email, setEmail] = useState("");
  const [pending, setPending] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setPending(true);
    try {
      const response = await buyerAuthPost("/password-reset/request", { email: email.trim() }, locale);
      if (response.status === 429) {
        const seconds = Number.parseInt(response.headers.get("Retry-After") ?? "60", 10);
        setError(tErrors("rateLimited", { seconds: Number.isFinite(seconds) ? seconds : 60 }));
      } else {
        // odpowiedź jest taka sama niezależnie od tego, czy konto istnieje
        setSent(true);
      }
    } catch {
      setError(tErrors("network"));
    } finally {
      setPending(false);
    }
  }

  return (
    <AccountColumn title={t("reset.requestTitle")} lead={t("reset.requestLead")}>
      {sent ? (
        <FormMessage tone="info">{t("reset.sent")}</FormMessage>
      ) : (
        <form onSubmit={submit} className="grid gap-5">
          <TextField label={t("email")} type="email" autoComplete="email" required maxLength={254} value={email} onChange={(e) => setEmail(e.target.value)} />
          {error && <FormMessage tone="error">{error}</FormMessage>}
          <SubmitButton pending={pending}>{t("reset.requestSubmit")}</SubmitButton>
        </form>
      )}
      <div className="mt-6 border-t border-line pt-6">
        <Link href="/account/login" className="text-small underline underline-offset-4 hover:no-underline">
          {t("verify.toLogin")}
        </Link>
      </div>
    </AccountColumn>
  );
}

function NewPasswordForm({ token }: { token: string }) {
  const t = useTranslations("account");
  const tErrors = useTranslations("errors");
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [state, setState] = useState<"form" | "saved" | "invalid">("form");
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    const length = [...password].length;
    if (length < 12 || length > 128) {
      setError(t("register.errors.password"));
      return;
    }
    setPending(true);
    try {
      const response = await buyerAuthPost("/password-reset/confirm", { token, password });
      if (response.ok) {
        setState("saved");
        return;
      }
      const problem = await readProblem(response);
      if (problem.code === "identity.password.policy") setError(t("register.errors.password"));
      else if (problem.code === "identity.buyer.token-invalid") setState("invalid");
      else setError(tErrors("generic"));
    } catch {
      setError(tErrors("network"));
    } finally {
      setPending(false);
    }
  }

  if (state !== "form") {
    return (
      <AccountColumn title={t("reset.newTitle")}>
        <FormMessage tone={state === "saved" ? "info" : "error"}>{state === "saved" ? t("reset.saved") : t("reset.invalid")}</FormMessage>
        <div className="mt-6 flex flex-wrap gap-x-6 gap-y-2 text-small">
          <Link href="/account/login" className="underline underline-offset-4 hover:no-underline">
            {t("verify.toLogin")}
          </Link>
          {state === "invalid" && (
            <Link href="/account/reset-password" className="underline underline-offset-4 hover:no-underline">
              {t("reset.requestSubmit")}
            </Link>
          )}
        </div>
      </AccountColumn>
    );
  }
  return (
    <AccountColumn title={t("reset.newTitle")}>
      <form onSubmit={submit} className="grid gap-5">
        <TextField
          label={t("password")}
          type="password"
          autoComplete="new-password"
          required
          maxLength={128}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          hint={t("passwordHint")}
        />
        {error && <FormMessage tone="error">{error}</FormMessage>}
        <SubmitButton pending={pending}>{t("reset.newSubmit")}</SubmitButton>
      </form>
    </AccountColumn>
  );
}
