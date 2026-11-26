import type { MetadataRoute } from "next";

/**
 * PWA katalogu dla kupców: „Dodaj do ekranu głównego” otwiera showroom jak aplikację (bez paska przeglądarki).
 * Panel marki ma osobny manifest (`public/admin.webmanifest`, zakres `/admin/`). Bez trybu offline — ceny i zestawienia
 * zawsze pochodzą z aktualnego API.
 */
export default function manifest(): MetadataRoute.Manifest {
  return {
    id: "/",
    name: "Fabian's Style — showroom hurtowy",
    short_name: "Fabian's Style",
    description: "Kolekcja hurtowa Fabian's Style: katalog, ceny dla kupców i zestawienia PDF.",
    lang: "pl",
    start_url: "/",
    scope: "/",
    display: "standalone",
    orientation: "portrait",
    background_color: "#ffffff",
    theme_color: "#ffffff",
    categories: ["business", "shopping"],
    icons: [
      { src: "/pwa/icon-192.png", sizes: "192x192", type: "image/png", purpose: "any" },
      { src: "/pwa/icon-512.png", sizes: "512x512", type: "image/png", purpose: "any" },
      { src: "/pwa/icon-maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  };
}
