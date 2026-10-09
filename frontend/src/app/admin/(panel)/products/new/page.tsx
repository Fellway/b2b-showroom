import type { Metadata } from "next";
import { ProductEditor } from "@/components/admin/product-editor/product-editor";

export const metadata: Metadata = { title: "Nowy model" };

export default function NewProductPage() {
  return <ProductEditor />;
}
