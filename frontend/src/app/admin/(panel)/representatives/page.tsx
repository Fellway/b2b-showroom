import type { Metadata } from "next";
import { RepresentativesPage } from "@/components/admin/representatives/representatives-page";

export const metadata: Metadata = { title: "Opiekunowie handlowi" };

export default function Page() {
  return <RepresentativesPage />;
}
