"use client";

import { X } from "lucide-react";
import { useTranslations } from "next-intl";
import { useEffect, useRef } from "react";
import { Button } from "@/components/ui/button";
import { consent, useConsent } from "@/lib/consent";
import { cn } from "@/lib/utils";

/**
 * Pytanie o zgodę na analitykę. Nie blokuje strony (katalog działa tak samo bez zgody), a obie odpowiedzi
 * mają równą wagę wizualną — odmowa jest tak samo łatwa jak zgoda. Wraca na żądanie z linku w stopce.
 */
/** Adres polityki prywatności marki — link pojawia się dopiero, gdy marka ją dostarczy (nie tworzymy treści prawnych). */
const PRIVACY_POLICY_URL = process.env.NEXT_PUBLIC_PRIVACY_POLICY_URL;

export function ConsentBanner() {
  const t = useTranslations("consent");
  const { state, settingsOpen } = useConsent();
  const firstAction = useRef<HTMLButtonElement>(null);
  const visible = state === "unknown" || settingsOpen;

  useEffect(() => {
    if (!settingsOpen) return;
    firstAction.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape" && consent.get() !== "unknown") consent.closeSettings();
    };
    addEventListener("keydown", onKey);
    return () => removeEventListener("keydown", onKey);
  }, [settingsOpen]);

  if (!visible) return null;

  return (
    <section
      role="region"
      aria-labelledby="consent-title"
      className={cn(
        "fixed inset-x-0 bottom-0 z-40 border-t border-line bg-background px-5 pb-5 pt-4 shadow-menu",
        "md:inset-x-auto md:bottom-8 md:left-8 md:w-[calc((100vw-96px)*2/3+16px)] md:border md:p-5",
        "lg:w-[calc((min(100vw,1600px)-112px)/4)]",
        "animate-in fade-in-0 slide-in-from-bottom-2 duration-300",
      )}
    >
      <div className="flex items-start justify-between gap-4">
        <h2 id="consent-title" className="text-small font-medium">
          {t("title")}
        </h2>
        {state !== "unknown" && (
          <Button variant="icon" size="icon-sm" className="-mr-2 -mt-1.5" aria-label={t("close")} onClick={() => consent.closeSettings()}>
            <X aria-hidden strokeWidth={1.5} />
          </Button>
        )}
      </div>
      <p className="mt-2 text-small text-ink-2">{t("body")}</p>
      <p className="mt-2 text-meta text-ink-3">
        {t("necessary")}
        {PRIVACY_POLICY_URL && (
          <>
            {" "}
            <a href={PRIVACY_POLICY_URL} className="text-ink-2 underline underline-offset-4 hover:text-foreground">
              {t("policy")}
            </a>
          </>
        )}
      </p>
      {state !== "unknown" && (
        <p className="mt-3 text-meta text-ink-2" aria-live="polite">
          {state === "granted" ? t("currentGranted") : t("currentDenied")}
        </p>
      )}
      <div className="mt-4 grid grid-cols-2 gap-2">
        <Button ref={firstAction} variant="secondary" onClick={() => consent.decide(false)}>
          {t("deny")}
        </Button>
        <Button variant="secondary" onClick={() => consent.decide(true)}>
          {t("grant")}
        </Button>
      </div>
    </section>
  );
}

export function ConsentSettingsLink({ className }: { className?: string }) {
  const t = useTranslations("consent");
  return (
    <span className="flex items-center gap-6">
      {PRIVACY_POLICY_URL && (
        <a href={PRIVACY_POLICY_URL} className={cn("underline-offset-4 hover:underline", className)}>
          {t("policy")}
        </a>
      )}
      <button
        type="button"
        onClick={(event) => consent.openSettings(event.currentTarget)}
        className={cn("underline-offset-4 hover:underline", className)}
      >
        {t("settings")}
      </button>
    </span>
  );
}
