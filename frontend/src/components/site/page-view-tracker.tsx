"use client";

import { useEffect } from "react";
import { useLocale } from "next-intl";
import { usePathname } from "@/i18n/navigation";
import { track } from "@/lib/analytics";

export function PageViewTracker() {
  const pathname = usePathname();
  const locale = useLocale();
  useEffect(() => {
    track({ type: "PAGE_VIEW", language: locale });
  }, [pathname, locale]);
  return null;
}
