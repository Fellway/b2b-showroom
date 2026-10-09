import type { Metadata } from "next";
import { LineSheetDetailView } from "@/components/admin/line-sheets/line-sheet-detail";

export const metadata: Metadata = { title: "Zestawienie" };

export default async function LineSheetPage({ params }: PageProps<"/admin/line-sheets/[id]">) {
  const { id } = await params;
  return <LineSheetDetailView id={id} />;
}
