import { cn } from "@/lib/utils";

export type ProductStatus = "DRAFT" | "PUBLISHED" | "HIDDEN" | "ARCHIVED";

export const PRODUCT_STATUS_LABEL: Record<ProductStatus, string> = {
  DRAFT: "Szkic",
  PUBLISHED: "Opublikowany",
  HIDDEN: "Ukryty",
  ARCHIVED: "Zarchiwizowany",
};

/**
 * Status bez koloru (DESIGN.md › Kolor-należy-do-kolekcji): pełna kropka = widoczny w katalogu,
 * pusta kropka = niewidoczny; tekst niesie znaczenie, kropka tylko je wzmacnia.
 */
export function StatusLabel({ status, className }: { status: ProductStatus; className?: string }) {
  const visible = status === "PUBLISHED";
  return (
    <span className={cn("inline-flex items-center gap-2 whitespace-nowrap", visible ? "text-foreground" : status === "ARCHIVED" ? "text-ink-3" : "text-ink-2", className)}>
      <span aria-hidden className={cn("size-2 rounded-full border border-foreground", visible ? "bg-foreground" : "bg-transparent", status === "ARCHIVED" && "border-ink-3")} />
      {PRODUCT_STATUS_LABEL[status]}
    </span>
  );
}

/** Stan włączony / wyłączony (opiekun, konto, widoczność kategorii) w tej samej gramatyce: tekst + kropka. */
export function ActiveLabel({ active, activeText = "Aktywny", inactiveText = "Nieaktywny" }: { active: boolean; activeText?: string; inactiveText?: string }) {
  return (
    <span className={active ? "inline-flex items-center gap-2" : "inline-flex items-center gap-2 text-ink-3"}>
      <span aria-hidden className={active ? "size-2 rounded-full border border-foreground bg-foreground" : "size-2 rounded-full border border-ink-3"} />
      {active ? activeText : inactiveText}
    </span>
  );
}
