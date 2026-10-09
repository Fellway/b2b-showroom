import type { Metadata } from "next";
import { ProductEditor } from "@/components/admin/product-editor/product-editor";

export const metadata: Metadata = { title: "Model" };

export default async function ProductEditPage({ params }: PageProps<"/admin/products/[id]">) {
  const { id } = await params;
  return <ProductEditor key={id} productId={id} />;
}
