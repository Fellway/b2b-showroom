import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

/**
 * Wewnętrzny adres API (Spring Boot). W produkcji frontend i API stoją za tym samym reverse proxy,
 * a ten rewrite odtwarza to samo zachowanie lokalnie: przeglądarka zawsze woła względne `/api/...`.
 */
const apiOrigin = process.env.API_INTERNAL_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
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
