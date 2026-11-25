"use client";

import { useLocale, useTranslations } from "next-intl";
import { useEffect, useState, type FormEvent } from "react";
import { Link } from "@/i18n/navigation";
import { buyerAuthPost } from "@/lib/buyer/session";
import { useHydrated } from "@/lib/use-hydrated";
import { AccountColumn, SubmitButton, TextField } from "./fields";
import { sendOnce, useTokenFromLink } from "./token-link";

type Result = "pending" | "done" | "invalid";

export function VerifyEmail() {
  const t = useTranslations("account");
  const hydrated = useHydrated();
  const token = useTokenFromLink();
  const [response, setResponse] = useState<Result>("pending");
  // brak tokenu w adresie (np. odświeżona strona — token usuwamy z paska adresu) = link nieważny
  const result: Result = hydrated && !token ? "invalid" : response;

  useEffect(() => {
    if (!hydrated || !token) return;
    let cancelled = false;
    sendOnce(`verify:${token}`, () => buyerAuthPost("/verify-email", { token }))
      .then((reply) => !cancelled && setResponse(reply.ok ? "done" : "invalid"))
      .catch(() => !cancelled && setResponse("invalid"));
    return () => {
      cancelled = true;
    };
  }, [hydrated, token]);

  if (result === "pending") {
    return (
      <AccountColumn title={t("titles.verify")}>
        <p aria-live="polite" className="text-ink-2">
          {t("verify.checking")}
        </p>
      </AccountColumn>
    );
  }
  if (result === "done") {
    return (
      <AccountColumn title={t("verify.doneTitle")} lead={t("verify.doneLead")}>
        <Link href="/" className="text-small underline underline-offset-4 hover:no-underline">
          {t("backToCatalog")}
        </Link>
      </AccountColumn>
    );
  }
  return (
    <AccountColumn title={t("verify.invalidTitle")} lead={t("verify.invalidLead")}>
      <ResendForm />
      <div className="mt-6 border-t border-line pt-6">
        <Link href="/account/login" className="text-small underline underline-offset-4 hover:no-underline">
          {t("verify.toLogin")}
        </Link>
      </div>
    </AccountColumn>
  );
}

function ResendForm() {
  const t = useTranslations("account");
  const locale = useLocale();
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [pending, setPending] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    await buyerAuthPost("/resend-verification", { email: email.trim() }, locale).catch(() => undefined);
    setPending(false);
    setSent(true);
  }

  if (sent) {
    return (
      <p aria-live="polite" className="bg-well px-4 py-3 text-small text-ink-2">
        {t("login.resent")}
      </p>
    );
  }
  return (
    <form onSubmit={submit} className="grid gap-4">
      <TextField label={t("email")} type="email" autoComplete="email" required maxLength={254} value={email} onChange={(e) => setEmail(e.target.value)} />
      <SubmitButton pending={pending}>{t("login.resend")}</SubmitButton>
    </form>
  );
}
