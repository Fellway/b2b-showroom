import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

/**
 * Wewnętrzny adres API (Spring Boot). W produkcji frontend i API stoją za tym samym reverse proxy,
 * a ten rewrite odtwarza to samo zachowanie lokalnie: przeglądarka zawsze woła względne `/api/...`.
 * Uwaga: rewrite jest zapisywany przy `next build` — obraz Dockera buduje się z adresem usługi `backend`.
 * W produkcji z rewrite'u korzysta optymalizator `next/image` (zdjęcia z `/api/v1/media/...`).
 */
const apiOrigin = process.env.API_INTERNAL_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  // samodzielny serwer (`.next/standalone/server.js`) do obrazu Dockera — bez pełnego node_modules
  output: "standalone",
  poweredByHeader: false,
  devIndicators: false,
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${apiOrigin}/api/:path*` }];
  },
  images: {
    // zdjęcia produktów serwuje backend pod /api/v1/media/products/** (klucze są niezmienne)
    localPatterns: [
      { pathname: "/api/v1/media/products/**", search: "" },
      { pathname: "/brand/**", search: "" },
    ],
    formats: ["image/avif", "image/webp"],
  },
};

const withNextIntl = createNextIntlPlugin("./src/i18n/request.ts");
export default withNextIntl(nextConfig);
