import type { Metadata } from "next";
import { Suspense } from "react";
import { LineSheetList } from "@/components/admin/line-sheets/line-sheet-list";

export const metadata: Metadata = { title: "Zestawienia" };

export default function LineSheetsPage() {
  return (
    <Suspense>
      <LineSheetList />
    </Suspense>
  );
}
