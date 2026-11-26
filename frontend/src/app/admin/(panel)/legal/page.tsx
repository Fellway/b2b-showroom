import type { Metadata } from "next";
import { Suspense } from "react";
import { LegalPage } from "@/components/admin/legal/legal-page";

export const metadata: Metadata = { title: "Dokumenty prawne" };

export default function Page() {
  return (
    <Suspense>
      <LegalPage />
    </Suspense>
  );
}
