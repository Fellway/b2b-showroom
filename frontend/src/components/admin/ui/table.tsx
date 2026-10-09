import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

/**
 * Tabela panelu wg DESIGN.md › Wiersz danych: nagłówki Meta ink-3 bez tła, wiersze rozdzielone linią górną,
 * bez linii pionowych. Liczby w kolumnach: `.tabular` + wyrównanie do prawej (`numeric`).
 */
export function Table({ className, ...props }: ComponentProps<"table">) {
  return (
    <div className="w-full overflow-x-auto">
      <table {...props} className={cn("w-full border-collapse text-small", className)} />
    </div>
  );
}

export function Th({ className, numeric, ...props }: ComponentProps<"th"> & { numeric?: boolean }) {
  return (
    <th
      scope="col"
      {...props}
      className={cn("whitespace-nowrap px-3 pb-2 pt-1 text-left text-meta font-normal text-ink-3 first:pl-0 last:pr-0", numeric && "text-right", className)}
    />
  );
}

export function Tr({ className, interactive, ...props }: ComponentProps<"tr"> & { interactive?: boolean }) {
  return (
    <tr
      {...props}
      className={cn("border-t border-line", interactive && "cursor-pointer transition-colors hover:bg-well/60 focus-within:bg-well/60", className)}
    />
  );
}

export function Td({ className, numeric, ...props }: ComponentProps<"td"> & { numeric?: boolean }) {
  return <td {...props} className={cn("px-3 py-3 align-middle first:pl-0 last:pr-0", numeric && "tabular text-right", className)} />;
}
