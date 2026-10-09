import type { Metadata } from "next";
import { CategoriesView } from "@/components/admin/dictionaries/categories-view";

export const metadata: Metadata = { title: "Kategorie" };

export default function Page() {
  return <CategoriesView />;
}
