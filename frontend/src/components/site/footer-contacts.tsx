"use client";

import { usePathname } from "@/i18n/navigation";
import type { SalesRepresentative } from "@/lib/types";
import { RepresentativeContact } from "./representative-contact";

/**
 * Sekcja kontaktu w stopce. Na karcie produktu ten sam kontakt stoi przy akcji głównej,
 * więc stopka pomija całą sekcję (nagłówek, zachętę i dane) zamiast ją powtarzać.
 */
export function FooterContacts({
  representatives,
  title,
  lead,
}: {
  representatives: SalesRepresentative[];
  title: string;
  lead: string;
}) {
  const pathname = usePathname();
  if (pathname.startsWith("/p/") || representatives.length === 0) return null;
  return (
    <>
      <div className="md:col-span-5">
        <h2 className="text-title font-medium text-balance">{title}</h2>
        <p className="mt-3 max-w-[46ch] text-ink-2">{lead}</p>
      </div>
      <div className="grid gap-8 sm:grid-cols-2 md:col-span-4">
        {representatives.map((rep) => (
          <RepresentativeContact key={rep.id} representative={rep} />
        ))}
      </div>
    </>
  );
}
