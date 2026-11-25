import Image from "next/image";
import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import type { Category } from "@/lib/types";
import { CategoryNav } from "./category-nav";
import { LocaleSwitcher } from "./locale-switcher";
import { SelectionButton } from "@/components/selection/selection-button";
import { AccountLink } from "@/components/account/account-link";

export async function SiteHeader({ categories }: { categories: Category[] }) {
  const t = await getTranslations("site");
  return (
    <header className="sticky top-0 z-30 border-b border-line bg-background">
      <div className="mx-auto flex h-(--header-height) max-w-[1600px] items-center gap-8 px-5 md:px-8">
        <Link href="/" aria-label={t("home")} className="shrink-0">
          <Image
            src="/brand/fabians-style-logo.png"
            alt="Fabian's Style"
            width={866}
            height={191}
            priority
            className="h-[26px] w-auto md:h-[30px]"
          />
        </Link>
        <CategoryNav categories={categories} className="hidden lg:flex" />
        <div className="ml-auto flex items-center gap-1">
          <LocaleSwitcher />
          <AccountLink />
          <SelectionButton />
        </div>
      </div>
      <CategoryNav
        categories={categories}
        className="flex overflow-x-auto border-t border-line px-5 md:px-8 lg:hidden [scrollbar-width:none] [mask-image:linear-gradient(to_right,black_85%,transparent)]"
      />
    </header>
  );
}
