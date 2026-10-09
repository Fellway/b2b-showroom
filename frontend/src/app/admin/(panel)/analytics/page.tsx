import type { Metadata } from "next";
import { Suspense } from "react";
import { AnalyticsDashboard } from "@/components/admin/analytics/analytics-dashboard";

export const metadata: Metadata = { title: "Statystyki" };

export default function AnalyticsPage() {
  return (
    <Suspense>
      <AnalyticsDashboard />
    </Suspense>
  );
}
