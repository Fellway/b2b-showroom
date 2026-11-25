/** Kształty odpowiedzi publicznego API `/api/v1` (zob. backend/README.md). Ceny i EAN — tylko w API kupca (D23). */

export type Price = { wholesale: number; retail: number; currency: string; markup: number };

export type ImageRef = { id: string; colorId: string | null; url: string; width: number | null; height: number | null };

export type ColorSwatch = { id: string; code: string; name: string; hex: string | null };

export type ProductCard = {
  id: string;
  slug: string;
  styleCode: string;
  name: string;
  categorySlug: string;
  featured: boolean;
  cover: ImageRef | null;
  colors: ColorSwatch[];
  sizes: string[];
};

export type VariantOption = { id: string; size: string; sku: string };

export type ColorOption = ColorSwatch & { variants: VariantOption[] };

export type ProductDetail = {
  id: string;
  slug: string;
  styleCode: string;
  name: string;
  description: string | null;
  materialComposition: string | null;
  careInstructions: string | null;
  category: { id: string; slug: string; name: string };
  colors: ColorOption[];
  images: ImageRef[];
};

export type Category = {
  id: string;
  parentId: string | null;
  slug: string;
  name: string;
  description: string | null;
  sortOrder: number;
};

export type Facets = {
  colors: (ColorSwatch & { productCount: number })[];
  sizes: { id: string; code: string; productCount: number }[];
};

export type Page<T> = { items: T[]; page: number; size: number; totalItems: number; totalPages: number };

export type SalesRepresentative = { id: string; fullName: string; email: string; phone: string | null };

export type CatalogSort = "RECOMMENDED" | "NEWEST" | "PRICE_ASC" | "PRICE_DESC";

export type CreatedLineSheet = { id: string; referenceNumber: string; pdfUrl: string };

/** Ceny i EAN produktu — `GET /api/v1/buyer/catalog/pricing`, tylko dla zalogowanego kupca. */
export type ProductPricing = { productId: string; price: Price; variants: { variantId: string; sku: string; ean: string }[] };

export type BuyerStatus = "PENDING_EMAIL" | "PENDING_APPROVAL" | "ACTIVE" | "REJECTED" | "SUSPENDED";

export type BuyerAccount = {
  id: string;
  email: string;
  companyName: string;
  countryCode: string;
  taxId: string;
  contactName: string;
  phone: string;
  language: string;
  status: BuyerStatus;
  createdAt: string;
  version: number;
};

export type BuyerLineSheet = {
  id: string;
  publicId: string;
  referenceNumber: string;
  language: string;
  itemCount: number;
  createdAt: string;
};

/** RFC 9457 Problem Details z własnym, stabilnym kodem błędu. */
export type Problem = { status: number; code?: string; detail?: string; errors?: { field: string; message: string }[] };
