"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { AdminShell } from "@/components/admin/shell/admin-shell";
import { useSession } from "@/lib/admin/use-session";

export default function PanelLayout({ children }: { children: ReactNode }) {
  const state = useSession();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (state.status === "anonymous") {
      router.replace(`/admin/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [state.status, router, pathname]);

  if (state.status !== "authenticated") {
    return <div className="min-h-dvh" aria-busy="true" />;
  }
  return <AdminShell admin={state.admin}>{children}</AdminShell>;
}
