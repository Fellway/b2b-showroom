"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  BarChart3,
  ExternalLink,
  FileText,
  KeyRound,
  LogOut,
  Palette,
  Ruler,
  Shirt,
  Tags,
  Users,
  type LucideIcon,
} from "lucide-react";
import type { ReactNode } from "react";
import { session, type AdminIdentity } from "@/lib/admin/session";
import { cn } from "@/lib/utils";

type NavItem = { href: string; label: string; icon: LucideIcon; adminOnly?: boolean };

const NAV: { title?: string; items: NavItem[] }[] = [
  {
    items: [
      { href: "/admin/products", label: "Modele", icon: Shirt },
      { href: "/admin/line-sheets", label: "Zestawienia", icon: FileText },
      { href: "/admin/analytics", label: "Statystyki", icon: BarChart3 },
    ],
  },
  {
    title: "Słowniki",
    items: [
      { href: "/admin/categories", label: "Kategorie", icon: Tags },
      { href: "/admin/colors", label: "Kolory", icon: Palette },
      { href: "/admin/sizes", label: "Rozmiary", icon: Ruler },
    ],
  },
  {
    title: "Ustawienia",
    items: [
      { href: "/admin/representatives", label: "Opiekunowie", icon: Users },
      { href: "/admin/users", label: "Konta", icon: KeyRound, adminOnly: true },
    ],
  },
];

const ROLE_LABEL = { ADMIN: "Administrator", EDITOR: "Redaktor" } as const;

/**
 * Powłoka panelu: stała nawigacja boczna (desktop). Układ jest dwukolumnowy tylko od `lg`; poniżej nawigacja
 * staje się górnym paskiem — panel w pierwszym etapie jest desktopowy, ale nie może się rozsypać na tablecie.
 */
export function AdminShell({ admin, children }: { admin: AdminIdentity; children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();

  async function logout() {
    await session.logout();
    router.replace("/admin/login");
  }

  return (
    <div className="min-h-dvh lg:grid lg:grid-cols-[232px_1fr]">
      <aside className="border-b border-line bg-background lg:sticky lg:top-0 lg:flex lg:h-dvh lg:flex-col lg:border-b-0 lg:border-r">
        <div className="flex h-(--header-height) items-center gap-3 border-b border-line px-6">
          <Link href="/admin/products" aria-label="Panel — modele" className="shrink-0">
            <Image src="/brand/fabians-style-logo.png" alt="Fabian's Style" width={866} height={191} className="h-6 w-auto" />
          </Link>
          <span className="text-meta text-ink-3">Panel</span>
        </div>
        <nav aria-label="Panel" className="flex gap-6 overflow-x-auto px-6 py-3 lg:flex-1 lg:flex-col lg:gap-6 lg:overflow-y-auto lg:py-6">
          {NAV.map((group, index) => (
            <div key={group.title ?? index} className="flex shrink-0 gap-1 lg:flex-col">
              {group.title && <p className="hidden px-2 pb-1 text-meta text-ink-3 lg:block">{group.title}</p>}
              {group.items
                .filter((item) => !item.adminOnly || admin.role === "ADMIN")
                .map((item) => {
                  const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
                  const Icon = item.icon;
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      aria-current={active ? "page" : undefined}
                      className={cn(
                        "flex h-9 shrink-0 items-center gap-2.5 px-2 transition-colors",
                        active ? "bg-foreground text-background" : "text-ink-2 hover:bg-well hover:text-foreground",
                      )}
                    >
                      <Icon aria-hidden className="size-4" strokeWidth={1.5} />
                      {item.label}
                    </Link>
                  );
                })}
            </div>
          ))}
        </nav>
        <div className="hidden border-t border-line px-6 py-4 lg:block">
          <p className="truncate font-medium">{admin.displayName}</p>
          {admin.displayName !== ROLE_LABEL[admin.role] && <p className="text-meta text-ink-3">{ROLE_LABEL[admin.role]}</p>}
          <p className="truncate text-meta text-ink-3" title={admin.email}>
            {admin.email}
          </p>
          <div className="mt-3 flex flex-col items-start gap-1">
            <a href="/" target="_blank" rel="noopener" className="inline-flex items-center gap-2 py-1 text-ink-2 hover:text-foreground">
              <ExternalLink aria-hidden className="size-4" strokeWidth={1.5} />
              Katalog publiczny
            </a>
            <button type="button" onClick={logout} className="inline-flex items-center gap-2 py-1 text-ink-2 hover:text-foreground">
              <LogOut aria-hidden className="size-4" strokeWidth={1.5} />
              Wyloguj
            </button>
          </div>
        </div>
      </aside>
      <div className="min-w-0">{children}</div>
    </div>
  );
}
