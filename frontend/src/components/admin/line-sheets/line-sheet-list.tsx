"use client";

import Link from "next/link";
import { Mail, Phone } from "lucide-react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { PageBody, PageHeader } from "@/components/admin/ui/page-header";
import { EmptyState, ErrorState, LoadingRows } from "@/components/admin/ui/states";
import { Table, Td, Th, Tr } from "@/components/admin/ui/table";
import { errorMessage } from "@/lib/admin/errors";
import { formatDateTime, LANGUAGE_NAME, useLineSheets } from "@/lib/admin/line-sheets";
import { PdfButton } from "./pdf-button";
import { Pager } from "./pager";

const plural = (n: number) => (n === 1 ? "zestawienie" : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 12 || n % 100 > 14) ? "zestawienia" : "zestawień");

/** Lista leadów: każde zestawienie to kupiec, który zbudował ofertę w katalogu. */
export function LineSheetList() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const page = Math.max(0, Number.parseInt(searchParams.get("page") ?? "1", 10) - 1 || 0);
  const { data, isPending, isError, error, refetch } = useLineSheets(page);

  const goTo = (next: number) => router.replace(next > 0 ? `${pathname}?page=${next + 1}` : pathname, { scroll: true });

  return (
    <>
      <PageHeader title="Zestawienia" meta={data ? `${data.totalItems} ${plural(data.totalItems)}` : undefined} />
      <PageBody>
        {isPending ? (
          <LoadingRows />
        ) : isError ? (
          <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
        ) : data.items.length === 0 ? (
          <EmptyState title="Nie ma jeszcze żadnych zestawień">
            Zestawienia pojawiają się tutaj, gdy zalogowany kupiec wybierze modele w katalogu i utworzy PDF. Każde to
            potencjalne zamówienie — z danymi firmy z konta kupca.
          </EmptyState>
        ) : (
          <>
            {/* telefon: karta leada — kontakt z kupcem jednym dotknięciem */}
            <ul className="divide-y divide-line border-y border-line md:hidden">
              {data.items.map((sheet) => (
                <li key={sheet.id} className="py-3.5">
                  <Link href={`/admin/line-sheets/${sheet.id}`} className="flex flex-col gap-1 active:bg-well">
                    <span className="flex items-baseline justify-between gap-3">
                      <span className="min-w-0 font-medium">{sheet.companyName ?? "Zestawienie anonimowe"}</span>
                      <span className="tabular shrink-0 text-meta text-ink-3">{formatDateTime(sheet.createdAt)}</span>
                    </span>
                    <span className="tabular text-meta text-ink-3">
                      {sheet.referenceNumber} · {sheet.itemCount} poz. · {LANGUAGE_NAME[sheet.language] ?? sheet.language}
                      {sheet.contactName && <> · {sheet.contactName}</>}
                    </span>
                  </Link>
                  <div className="mt-2 flex flex-wrap items-center gap-2">
                    {sheet.phone && (
                      <a
                        href={`tel:${sheet.phone.replace(/[^\d+]/g, "")}`}
                        className="inline-flex h-10 items-center gap-2 border border-edge px-3 text-small"
                      >
                        <Phone aria-hidden className="size-4" strokeWidth={1.5} />
                        Zadzwoń
                      </a>
                    )}
                    {sheet.email && (
                      <a href={`mailto:${sheet.email}`} className="inline-flex h-10 items-center gap-2 border border-edge px-3 text-small">
                        <Mail aria-hidden className="size-4" strokeWidth={1.5} />
                        E-mail
                      </a>
                    )}
                    <span className="ml-auto">
                      <PdfButton id={sheet.id} referenceNumber={sheet.referenceNumber} />
                    </span>
                  </div>
                </li>
              ))}
            </ul>
            <Table className="hidden md:table">
              <thead>
                <tr>
                  <Th>Numer</Th>
                  <Th>Utworzono</Th>
                  <Th>Firma</Th>
                  <Th>Osoba kontaktowa</Th>
                  <Th>Kontakt</Th>
                  <Th numeric>Pozycje</Th>
                  <Th>Język</Th>
                  <Th className="sr-only">PDF</Th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((sheet) => (
                  <Tr key={sheet.id} interactive onClick={() => router.push(`/admin/line-sheets/${sheet.id}`)}>
                    <Td>
                      <Link
                        href={`/admin/line-sheets/${sheet.id}`}
                        onClick={(e) => e.stopPropagation()}
                        className="tabular font-medium underline-offset-4 hover:underline"
                      >
                        {sheet.referenceNumber}
                      </Link>
                    </Td>
                    <Td className="tabular whitespace-nowrap text-ink-2">{formatDateTime(sheet.createdAt)}</Td>
                    <Td>{sheet.companyName ?? <span className="text-ink-3">—</span>}</Td>
                    <Td>{sheet.contactName ?? <span className="text-ink-3">—</span>}</Td>
                    <Td>
                      {sheet.email || sheet.phone ? (
                        <div className="flex flex-col gap-0.5">
                          {sheet.email && (
                            <a
                              href={`mailto:${sheet.email}`}
                              onClick={(e) => e.stopPropagation()}
                              className="break-all text-ink-2 underline-offset-4 hover:text-foreground hover:underline"
                            >
                              {sheet.email}
                            </a>
                          )}
                          {sheet.phone && (
                            <a
                              href={`tel:${sheet.phone.replace(/[^\d+]/g, "")}`}
                              onClick={(e) => e.stopPropagation()}
                              className="tabular text-ink-2 underline-offset-4 hover:text-foreground hover:underline"
                            >
                              {sheet.phone}
                            </a>
                          )}
                        </div>
                      ) : (
                        <span className="text-ink-3">anonimowe</span>
                      )}
                    </Td>
                    <Td numeric>{sheet.itemCount}</Td>
                    <Td className="text-ink-2">{LANGUAGE_NAME[sheet.language] ?? sheet.language}</Td>
                    <Td className="w-10 text-right">
                      <PdfButton id={sheet.id} referenceNumber={sheet.referenceNumber} compact />
                    </Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
            <Pager page={data.page} totalPages={data.totalPages} onChange={goTo} />
          </>
        )}
      </PageBody>
    </>
  );
}
