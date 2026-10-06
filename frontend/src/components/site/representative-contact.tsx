"use client";

import { Mail, Phone } from "lucide-react";
import { useLocale } from "next-intl";
import { track } from "@/lib/analytics";
import type { SalesRepresentative } from "@/lib/types";
import { cn } from "@/lib/utils";

/** Dane opiekuna jako bezpośrednie akcje (telefon / e-mail) — kontakt to jeden z dwóch celów wizyty kupca. */
export function RepresentativeContact({
  representative,
  className,
}: {
  representative: SalesRepresentative;
  className?: string;
}) {
  const locale = useLocale();
  const onClick = (channel: string) =>
    track({ type: "CONTACT_CLICKED", language: locale, properties: { channel } });

  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <p className="font-medium">{representative.fullName}</p>
      {representative.phone && (
        <a
          href={`tel:${representative.phone.replace(/[^\d+]/g, "")}`}
          onClick={() => onClick("phone")}
          className="tabular inline-flex items-center gap-2 text-ink-2 underline-offset-4 hover:text-foreground hover:underline"
        >
          <Phone aria-hidden className="size-4" strokeWidth={1.5} />
          {representative.phone}
        </a>
      )}
      <a
        href={`mailto:${representative.email}`}
        onClick={() => onClick("email")}
        className="inline-flex items-center gap-2 break-all text-ink-2 underline-offset-4 hover:text-foreground hover:underline"
      >
        <Mail aria-hidden className="size-4 shrink-0" strokeWidth={1.5} />
        {representative.email}
      </a>
    </div>
  );
}
