import type { Metadata, Viewport } from "next";
import { Golos_Text } from "next/font/google";
import type { ReactNode } from "react";
import { AdminProviders } from "@/components/admin/providers";
import "../globals.css";

const golos = Golos_Text({
  subsets: ["latin", "latin-ext", "cyrillic", "cyrillic-ext"],
  weight: ["400", "500"],
  variable: "--font-golos",
  display: "swap",
});

export const metadata: Metadata = {
  title: { default: "Panel · Fabian's Style", template: "%s · Panel · Fabian's Style" },
  robots: { index: false, follow: false },
  icons: { icon: "/brand/favicon.png", apple: "/pwa/apple-touch-icon.png" },
  // panel instalowany na telefonie otwiera się na zestawieniach (osobny manifest, zakres /admin/)
  manifest: "/admin.webmanifest",
  appleWebApp: { capable: true, title: "FS Panel", statusBarStyle: "default" },
};

export const viewport: Viewport = { themeColor: "#ffffff", viewportFit: "cover" };

export default function AdminRootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="pl" className={golos.variable}>
      <body className="min-h-dvh bg-background text-small">
        <AdminProviders>{children}</AdminProviders>
      </body>
    </html>
  );
}
