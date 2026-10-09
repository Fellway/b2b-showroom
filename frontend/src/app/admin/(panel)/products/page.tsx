import type { Metadata } from "next";
import { Suspense } from "react";
import { ProductsView } from "@/components/admin/product-list/products-view";

export const metadata: Metadata = { title: "Modele" };

export default function ProductsPage() {
  return (
    <Suspense>
      <ProductsView />
    </Suspense>
  );
}
