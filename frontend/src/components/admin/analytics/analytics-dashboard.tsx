"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { PageBody, PageHeader } from "@/components/admin/ui/page-header";
import { ErrorState, LoadingRows } from "@/components/admin/ui/states";
import { Table, Td, Th, Tr } from "@/components/admin/ui/table";
import { useAnalyticsSummary, type AnalyticsSummary, type RankingEntry } from "@/lib/admin/analytics";
import { errorMessage } from "@/lib/admin/errors";
import { cn } from "@/lib/utils";

const PRESETS = [7, 30, 90] as const;
const MAX_DAYS = 366;
const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** Data YYYY-MM-DD w strefie Europe/Warsaw (dni raportu odpowiadają kalendarzowi marki). */
const isoDay = (date: Date) => new Intl.DateTimeFormat("sv-SE", { timeZone: "Europe/Warsaw" }).format(date);

function rangeFromParams(params: URLSearchParams) {
  const from = params.get("from");
  const to = params.get("to");
  if (from && to && DATE.test(from) && DATE.test(to) && from <= to) return { from, to, preset: null };
  const days = PRESETS.find((p) => String(p) === params.get("days")) ?? 30;
  const end = new Date();
  const start = new Date(end.getTime() - (days - 1) * 86_400_000);
  return { from: isoDay(start), to: isoDay(end), preset: days };
}

const formatDay = (day: string) =>
  new Intl.DateTimeFormat("pl-PL", { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" }).format(new Date(`${day}T00:00:00Z`));

const number = (value: number) => new Intl.NumberFormat("pl-PL").format(value);

export function AnalyticsDashboard() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const range = rangeFromParams(new URLSearchParams(searchParams));
  const { data, isPending, isError, error, refetch, isFetching } = useAnalyticsSummary(range.from, range.to);

  const [customFrom, setCustomFrom] = useState(range.from);
  const [customTo, setCustomTo] = useState(range.to);
  const [rangeError, setRangeError] = useState<string | null>(null);

  function applyCustom(event: FormEvent) {
    event.preventDefault();
    if (!customFrom || !customTo || customFrom > customTo) {
      setRangeError("Data początkowa musi być wcześniejsza niż końcowa.");
      return;
    }
    if ((Date.parse(customTo) - Date.parse(customFrom)) / 86_400_000 + 1 > MAX_DAYS) {
      setRangeError(`Zakres może mieć najwyżej ${MAX_DAYS} dni.`);
      return;
    }
    setRangeError(null);
    router.replace(`${pathname}?from=${customFrom}&to=${customTo}`);
  }

  function preset(days: number) {
    router.replace(days === 30 ? pathname : `${pathname}?days=${days}`);
    const end = new Date();
    setCustomFrom(isoDay(new Date(end.getTime() - (days - 1) * 86_400_000)));
    setCustomTo(isoDay(end));
    setRangeError(null);
  }

  return (
    <>
      <PageHeader
        title="Statystyki"
        meta={`${formatDay(range.from)} – ${formatDay(range.to)}`}
        actions={
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
            <div role="group" aria-label="Zakres" className="flex">
              {PRESETS.map((days) => (
                <button
                  key={days}
                  type="button"
                  aria-pressed={range.preset === days}
                  onClick={() => preset(days)}
                  className={cn(
                    "tabular h-8 border px-3 text-meta transition-colors -ml-px first:ml-0",
                    range.preset === days ? "relative z-10 border-foreground bg-foreground text-background" : "border-edge hover:border-foreground hover:z-10 relative",
                  )}
                >
                  {days} dni
                </button>
              ))}
            </div>
            <form onSubmit={applyCustom} className="flex items-center gap-2" aria-label="Własny zakres">
              <label className="sr-only" htmlFor="range-from">
                Od
              </label>
              <input
                id="range-from"
                type="date"
                value={customFrom}
                max={customTo}
                onChange={(e) => setCustomFrom(e.target.value)}
                className="tabular h-8 border border-edge bg-background px-2 text-meta outline-none hover:border-foreground focus-visible:border-foreground"
              />
              <span aria-hidden className="text-ink-3">
                –
              </span>
              <label className="sr-only" htmlFor="range-to">
                Do
              </label>
              <input
                id="range-to"
                type="date"
                value={customTo}
                min={customFrom}
                onChange={(e) => setCustomTo(e.target.value)}
                className="tabular h-8 border border-edge bg-background px-2 text-meta outline-none hover:border-foreground focus-visible:border-foreground"
              />
              <Button type="submit" variant="secondary" size="sm">
                Pokaż
              </Button>
            </form>
          </div>
        }
      />
      <PageBody>
        {rangeError && (
          <p role="alert" className="mb-4 text-danger">
            {rangeError}
          </p>
        )}
        {isPending ? (
          <LoadingRows rows={4} />
        ) : isError ? (
          <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
        ) : (
          <div className={cn("transition-opacity", isFetching && "opacity-60")} aria-busy={isFetching}>
            <Totals summary={data} />
            <div className="mt-12 grid gap-12 xl:grid-cols-3">
              <Ranking
                title="Najczęściej oglądane"
                unit="Wyświetlenia"
                entries={data.topViewedProducts}
                empty="Kupcy nie otworzyli jeszcze żadnej karty produktu w tym okresie."
              />
              <Ranking
                title="Najczęściej dodawane do zestawień"
                unit="Dodania"
                entries={data.topSelectedProducts}
                empty="Nikt nie dodał jeszcze modelu do zestawienia w tym okresie."
              />
              <Ranking
                title="Najczęściej w pobranych zestawieniach"
                unit="Zestawienia"
                entries={data.topLineSheetProducts}
                empty="W tym okresie nie wygenerowano zestawień PDF."
              />
            </div>
          </div>
        )}
      </PageBody>
    </>
  );
}

/** Jeden cichy rząd danych (DESIGN.md › Wiersz danych) zamiast kafelków z dużymi liczbami. */
function Totals({ summary }: { summary: AnalyticsSummary }) {
  const t = summary.eventTotals;
  const items: { label: string; value: number; hint?: string }[] = [
    { label: "Zestawienia PDF", value: summary.lineSheetsCreated },
    { label: "Odsłony stron", value: t.PAGE_VIEW ?? 0 },
    { label: "Karty produktów", value: t.PRODUCT_VIEW ?? 0 },
    { label: "Dodania do zestawień", value: t.SELECTION_ADDED ?? 0 },
    { label: "Usunięcia z zestawień", value: t.SELECTION_REMOVED ?? 0 },
    { label: "Kliknięcia kontaktu", value: t.CONTACT_CLICKED ?? 0 },
  ];
  return (
    <section aria-label="Podsumowanie">
      <dl className="grid max-w-3xl grid-cols-1 gap-x-12 border-b border-line sm:grid-cols-2">
        {items.map((item) => (
          <div key={item.label} className="flex items-baseline justify-between gap-6 border-t border-line py-2.5">
            <dt className="text-ink-3">{item.label}</dt>
            <dd className="tabular font-medium">{number(item.value)}</dd>
          </div>
        ))}
      </dl>
      <p role="note" className="mt-4 max-w-3xl bg-well px-5 py-2 text-meta text-ink-2">
        Unikalnych odwiedzających nie liczymy: katalog nie zapisuje trwałego identyfikatora przeglądarki, dopóki nie
        zapadnie decyzja o zgodzie (ePrivacy). Odsłony liczone są per sesja karty.
      </p>
    </section>
  );
}

function Ranking({ title, unit, entries, empty }: { title: string; unit: string; entries: RankingEntry[]; empty: string }) {
  const max = Math.max(1, ...entries.map((e) => e.count));
  return (
    <section aria-label={title} className="min-w-0">
      <h2 className="mb-3 font-medium">{title}</h2>
      {entries.length === 0 ? (
        <p className="border-t border-line py-6 text-ink-2">{empty}</p>
      ) : (
        <Table>
          <thead>
            <tr>
              <Th className="w-8">#</Th>
              <Th>Model</Th>
              <Th numeric>
                <span className="sr-only">Liczba </span>
                {unit}
              </Th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry, index) => (
              <Tr key={entry.productId}>
                <Td className="tabular w-8 align-top text-ink-3">{index + 1}</Td>
                <Td className="w-full min-w-0 max-w-0">
                  {entry.known ? (
                    <Link href={`/admin/products/${entry.productId}`} className="block truncate font-medium underline-offset-4 hover:underline">
                      {entry.name}
                    </Link>
                  ) : (
                    <span className="text-ink-3">Usunięty model</span>
                  )}
                  {entry.styleCode && <span className="tabular block text-meta text-ink-3">{entry.styleCode}</span>}
                  <span aria-hidden className="mt-1.5 block h-0.5 bg-line">
                    <span className="block h-full bg-foreground" style={{ width: `${(entry.count / max) * 100}%` }} />
                  </span>
                </Td>
                <Td numeric className="align-top font-medium">
                  {number(entry.count)}
                </Td>
              </Tr>
            ))}
          </tbody>
        </Table>
      )}
    </section>
  );
}
