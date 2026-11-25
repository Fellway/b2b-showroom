import type { Metadata } from "next";
import { Suspense } from "react";
import { BuyersPage } from "@/components/admin/buyers/buyers-page";

export const metadata: Metadata = { title: "Kupcy" };

export default function Page() {
  return (
    <Suspense>
      <BuyersPage />
    </Suspense>
  );
}
