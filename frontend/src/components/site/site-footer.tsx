import Image from "next/image";
import { getTranslations } from "next-intl/server";
import type { SalesRepresentative } from "@/lib/types";
import { FooterContacts } from "./footer-contacts";
import { ConsentSettingsLink } from "./consent-banner";

const SOCIAL = [
  { label: "Facebook", href: "https://www.facebook.com/fabiansstyle" },
  { label: "Instagram", href: "https://www.instagram.com/fabiansstyle/" },
];

export async function SiteFooter({ representatives }: { representatives: SalesRepresentative[] }) {
  const t = await getTranslations("site");
  return (
    <footer className="mt-24 border-t border-line">
      <div className="mx-auto grid max-w-[1600px] gap-12 px-5 py-14 md:grid-cols-12 md:px-8">
        <FooterContacts
          representatives={representatives}
          title={t("footer.contactTitle")}
          lead={t("footer.contactLead")}
        />
        <div className="md:col-span-3">
          <h2 className="text-small text-ink-3">{t("footer.social")}</h2>
          <ul className="mt-3 flex flex-col gap-1.5">
            {SOCIAL.map((s) => (
              <li key={s.href}>
                <a href={s.href} target="_blank" rel="noopener noreferrer" className="underline-offset-4 hover:underline">
                  {s.label}
                </a>
              </li>
            ))}
          </ul>
        </div>
      </div>
      <div className="mx-auto flex max-w-[1600px] flex-wrap items-center justify-between gap-x-6 gap-y-3 border-t border-line px-5 py-6 text-meta text-ink-3 md:px-8">
        <Image src="/brand/fabians-style-logo.png" alt="" width={866} height={191} className="h-5 w-auto opacity-80" />
        <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
          <ConsentSettingsLink />
          <p>{t("footer.rights", { year: new Date().getFullYear() })}</p>
        </div>
      </div>
    </footer>
  );
}
