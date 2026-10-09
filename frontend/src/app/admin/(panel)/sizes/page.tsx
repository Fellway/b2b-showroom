import type { Metadata } from "next";
import { SizesView } from "@/components/admin/dictionaries/sizes-view";

export const metadata: Metadata = { title: "Rozmiary" };

export default function Page() {
  return <SizesView />;
}
