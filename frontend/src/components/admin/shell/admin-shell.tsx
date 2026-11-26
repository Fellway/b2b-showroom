"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  BarChart3,
  ExternalLink,
  FileText,
  KeyRound,
  Landmark,
  LogOut,
  Menu,
  Palette,
  Ruler,
  Shirt,
  Store,
  Tags,
  Users,
  type LucideIcon,
} from "lucide-react";
import { useState, type ReactNode } from "react";
import { Sheet, SheetContent, SheetTitle } from "@/components/ui/sheet";
import { usePendingBuyersCount } from "@/lib/admin/buyers";
import { session, type AdminIdentity } from "@/lib/admin/session";
import { cn } from "@/lib/utils";

type NavItem = { href: string; label: string; icon: LucideIcon; adminOnly?: boolean; badge?: "pendingBuyers" };

const NAV: { title?: string; items: NavItem[] }[] = [
  {
    items: [
      { href: "/admin/products", label: "Modele", icon: Shirt },
      { href: "/admin/line-sheets", label: "Zestawienia", icon: FileText },
      { href: "/admin/buyers", label: "Kupcy", icon: Store, badge: "pendingBuyers" },
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
      { href: "/admin/legal", label: "Dokumenty prawne", icon: Landmark },
      { href: "/admin/users", label: "Konta", icon: KeyRound, adminOnly: true },
    ],
  },
];

/** Na telefonie w dolnym pasku tylko to, co marka robi w biegu: zestawienia (leady), wnioski kupców, modele. */
const MOBILE_TABS: NavItem[] = [NAV[0].items[1], NAV[0].items[2], NAV[0].items[0]];

const ROLE_LABEL = { ADMIN: "Administrator", EDITOR: "Redaktor" } as const;

const isActive = (pathname: string, href: string) => pathname === href || pathname.startsWith(`${href}/`);

/**
 * Powłoka panelu. Od `lg`: stała nawigacja boczna. Poniżej (telefon, tablet): górny pasek z logo i menu, dolny pasek
 * z trzema najczęstszymi sekcjami (zestawienia, kupcy, modele) i „Menu” z pełną nawigacją w wysuwanym panelu.
 */
export function AdminShell({ admin, children }: { admin: AdminIdentity; children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const pendingBuyers = usePendingBuyersCount().data ?? 0;
  const [menuOpen, setMenuOpen] = useState(false);

  async function logout() {
    await session.logout();
    router.replace("/admin/login");
  }

  const account = (
    <div className="border-t border-line px-6 py-4">
      <p className="truncate font-medium">{admin.displayName}</p>
      {admin.displayName !== ROLE_LABEL[admin.role] && <p className="text-meta text-ink-3">{ROLE_LABEL[admin.role]}</p>}
      <p className="truncate text-meta text-ink-3" title={admin.email}>
        {admin.email}
      </p>
      <div className="mt-3 flex flex-col items-start gap-1">
        <a href="/" target="_blank" rel="noopener" className="inline-flex min-h-10 items-center gap-2 text-ink-2 hover:text-foreground lg:min-h-0 lg:py-1">
          <ExternalLink aria-hidden className="size-4" strokeWidth={1.5} />
          Katalog publiczny
        </a>
        <button type="button" onClick={logout} className="inline-flex min-h-10 items-center gap-2 text-ink-2 hover:text-foreground lg:min-h-0 lg:py-1">
          <LogOut aria-hidden className="size-4" strokeWidth={1.5} />
          Wyloguj
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-dvh pb-[calc(3.5rem+env(safe-area-inset-bottom))] lg:grid lg:grid-cols-[232px_1fr] lg:pb-0">
      {/* telefon / tablet: górny pasek */}
      <div className="flex h-14 items-center gap-3 border-b border-line bg-background px-5 lg:hidden">
        <Link href="/admin/line-sheets" aria-label="Panel — zestawienia" className="shrink-0">
          <Image src="/brand/fabians-style-logo.png" alt="Fabian's Style" width={866} height={191} className="h-5 w-auto" />
        </Link>
        <span className="text-meta text-ink-3">Panel</span>
        <button
          type="button"
          onClick={() => setMenuOpen(true)}
          aria-label="Menu panelu"
          aria-haspopup="dialog"
          className="-mr-2 ml-auto flex size-11 items-center justify-center text-ink-2 hover:text-foreground"
        >
          <Menu aria-hidden className="size-5" strokeWidth={1.5} />
        </button>
      </div>

      {/* desktop: nawigacja boczna */}
      <aside className="hidden border-r border-line bg-background lg:sticky lg:top-0 lg:flex lg:h-dvh lg:flex-col">
        <div className="flex h-(--header-height) items-center gap-3 border-b border-line px-6">
          <Link href="/admin/products" aria-label="Panel — modele" className="shrink-0">
            <Image src="/brand/fabians-style-logo.png" alt="Fabian's Style" width={866} height={191} className="h-6 w-auto" />
          </Link>
          <span className="text-meta text-ink-3">Panel</span>
        </div>
        <NavList admin={admin} pathname={pathname} pendingBuyers={pendingBuyers} className="flex-1 overflow-y-auto py-6" />
        {account}
      </aside>

      <div className="min-w-0">{children}</div>

      {/* telefon / tablet: dolny pasek z najczęstszymi sekcjami */}
      <nav
        aria-label="Najczęstsze sekcje"
        className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-4 border-t border-line bg-background pb-[env(safe-area-inset-bottom)] lg:hidden"
      >
        {MOBILE_TABS.map((item) => {
          const active = isActive(pathname, item.href);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={active ? "page" : undefined}
              className={cn("relative flex h-14 flex-col items-center justify-center gap-1 text-meta", active ? "text-foreground" : "text-ink-3")}
            >
              {active && <span aria-hidden className="absolute inset-x-6 top-0 h-0.5 bg-foreground" />}
              <span className="relative">
                <Icon aria-hidden className="size-5" strokeWidth={1.5} />
                {item.badge === "pendingBuyers" && pendingBuyers > 0 && (
                  <span
                    aria-label={`${pendingBuyers} do akceptacji`}
                    className="tabular absolute -right-3 -top-1.5 min-w-4 bg-foreground px-1 text-center text-[0.625rem] leading-4 text-background"
                  >
                    {pendingBuyers}
                  </span>
                )}
              </span>
              {item.label}
            </Link>
          );
        })}
        <button
          type="button"
          onClick={() => setMenuOpen(true)}
          aria-haspopup="dialog"
          className="flex h-14 flex-col items-center justify-center gap-1 text-meta text-ink-3"
        >
          <Menu aria-hidden className="size-5" strokeWidth={1.5} />
          Więcej
        </button>
      </nav>

      <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
        <SheetContent side="left" className="w-[86vw] max-w-[320px] gap-0 border-line p-0 shadow-panel lg:hidden">
          <SheetTitle className="flex h-14 items-center border-b border-line px-6 text-small font-medium">Menu panelu</SheetTitle>
          <NavList
            admin={admin}
            pathname={pathname}
            pendingBuyers={pendingBuyers}
            onNavigate={() => setMenuOpen(false)}
            className="flex-1 overflow-y-auto py-5"
            touch
          />
          {account}
        </SheetContent>
      </Sheet>
    </div>
  );
}

function NavList({
  admin,
  pathname,
  pendingBuyers,
  onNavigate,
  className,
  touch,
}: {
  admin: AdminIdentity;
  pathname: string;
  pendingBuyers: number;
  onNavigate?: () => void;
  className?: string;
  /** Większe pola dotyku (menu na telefonie). */
  touch?: boolean;
}) {
  return (
    <nav aria-label="Panel" className={cn("flex flex-col gap-6 px-6", className)}>
      {NAV.map((group, index) => (
        <div key={group.title ?? index} className="flex flex-col gap-1">
          {group.title && <p className="px-2 pb-1 text-meta text-ink-3">{group.title}</p>}
          {group.items
            .filter((item) => !item.adminOnly || admin.role === "ADMIN")
            .map((item) => {
              const active = isActive(pathname, item.href);
              const Icon = item.icon;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={onNavigate}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "flex shrink-0 items-center gap-2.5 px-2 transition-colors",
                    touch ? "h-11" : "h-9",
                    active ? "bg-foreground text-background" : "text-ink-2 hover:bg-well hover:text-foreground",
                  )}
                >
                  <Icon aria-hidden className="size-4" strokeWidth={1.5} />
                  {item.label}
                  {item.badge === "pendingBuyers" && pendingBuyers > 0 && (
                    <span
                      className={cn(
                        "tabular ml-auto min-w-5 px-1.5 text-center text-meta",
                        active ? "bg-background text-foreground" : "bg-foreground text-background",
                      )}
                      aria-label={`${pendingBuyers} do akceptacji`}
                    >
                      {pendingBuyers}
                    </span>
                  )}
                </Link>
              );
            })}
        </div>
      ))}
    </nav>
  );
}
