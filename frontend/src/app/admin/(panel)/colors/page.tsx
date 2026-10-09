import type { Metadata } from "next";
import { ColorsView } from "@/components/admin/dictionaries/colors-view";

export const metadata: Metadata = { title: "Kolory" };

export default function Page() {
  return <ColorsView />;
}
