"use client";

import Image from "next/image";
import Link from "next/link";
import { Download, Mail, Phone } from "lucide-react";
import type { ReactNode } from "react";
import { PageBody, PageHeader } from "@/components/admin/ui/page-header";
import { ErrorState, LoadingRows } from "@/components/admin/ui/states";
import { Table, Td, Th, Tr } from "@/components/admin/ui/table";
import { buttonVariants } from "@/components/ui/button";
import { errorMessage } from "@/lib/admin/errors";
import { formatDateTime, formatMoney, LANGUAGE_NAME, mediaUrl, pdfUrl, useLineSheet } from "@/lib/admin/line-sheets";
import { cn } from "@/lib/utils";

export function LineSheetDetailView({ id }: { id: string }) {
  const { data: sheet, isPending, isError, error, refetch } = useLineSheet(id);
  const back = { href: "/admin/line-sheets", label: "Zestawienia" };

  if (isPending) {
    return (
      <>
        <PageHeader title="Zestawienie" back={back} />
        <PageBody>
          <LoadingRows />
        </PageBody>
      </>
    );
  }
  if (isError) {
    return (
      <>
        <PageHeader title="Zestawienie" back={back} />
        <PageBody>
          <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
        </PageBody>
      </>
    );
  }

  const { recipient, items, currency } = sheet;
  const units = items.reduce((sum, item) => sum + (item.quantity ?? 0), 0);
  const withQuantity = items.filter((item) => item.quantity != null).length;
  const money = (value: number) => formatMoney(value, currency);

  return (
    <>
      <PageHeader
        title={<span className="tabular">{sheet.referenceNumber}</span>}
        meta={`${formatDateTime(sheet.createdAt)} · ${LANGUAGE_NAME[sheet.language] ?? sheet.language}`}
        back={back}
        actions={
          sheet.pdfAvailable ? (
            <a href={pdfUrl(sheet.publicId)} download className={cn(buttonVariants({ variant: "primary", size: "md" }))}>
              <Download aria-hidden strokeWidth={1.5} />
              Pobierz PDF
            </a>
          ) : (
            <span className="text-meta text-ink-3">PDF niedostępny</span>
          )
        }
      />
      <PageBody>
        <div className="flex flex-col gap-12">
          <div className="grid gap-x-12 gap-y-8 md:grid-cols-3">
            <InfoBlock title="Kupiec">
              {recipient.companyName || recipient.contactName || recipient.email || recipient.phone ? (
                <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2">
                  <Row label="Firma">{recipient.companyName}</Row>
                  <Row label="Osoba">{recipient.contactName}</Row>
                  <Row label="E-mail">
                    {recipient.email && (
                      <a href={`mailto:${recipient.email}?subject=${encodeURIComponent(`Zestawienie ${sheet.referenceNumber}`)}`} className="inline-flex items-center gap-1.5 break-all underline-offset-4 hover:underline">
                        <Mail aria-hidden className="size-3.5 shrink-0 text-ink-3" strokeWidth={1.5} />
                        {recipient.email}
                      </a>
                    )}
                  </Row>
                  <Row label="Telefon">
                    {recipient.phone && (
                      <a href={`tel:${recipient.phone.replace(/[^\d+]/g, "")}`} className="tabular inline-flex items-center gap-1.5 underline-offset-4 hover:underline">
                        <Phone aria-hidden className="size-3.5 shrink-0 text-ink-3" strokeWidth={1.5} />
                        {recipient.phone}
                      </a>
                    )}
                  </Row>
                  <Row label="NIP / VAT">{recipient.taxId && <span className="tabular">{recipient.taxId}</span>}</Row>
                  <Row label="Zgoda RODO">{sheet.privacyAcceptedAt && <span className="tabular">{formatDateTime(sheet.privacyAcceptedAt)}</span>}</Row>
                </dl>
              ) : (
                <p className="text-ink-2">Kupiec nie podał danych kontaktowych — zestawienie anonimowe.</p>
              )}
            </InfoBlock>

            {sheet.message && (
              <InfoBlock title="Wiadomość">
                <p className="whitespace-pre-line text-ink-2">{sheet.message}</p>
              </InfoBlock>
            )}

            <InfoBlock title="Opiekun handlowy">
              {sheet.salesRepresentative ? (
                <div className="flex flex-col gap-1">
                  <p className="font-medium">{sheet.salesRepresentative.fullName}</p>
                  <p className="text-ink-2">{sheet.salesRepresentative.email}</p>
                  {sheet.salesRepresentative.phone && <p className="tabular text-ink-2">{sheet.salesRepresentative.phone}</p>}
                </div>
              ) : (
                <p className="text-ink-2">Nie przypisano opiekuna.</p>
              )}
            </InfoBlock>
          </div>
          <section aria-labelledby="items-title" className="min-w-0">
            <h2 id="items-title" className="mb-3 font-medium">
              Pozycje <span className="tabular font-normal text-ink-3">{items.length}</span>
            </h2>
            <Table>
              <thead>
                <tr>
                  <Th className="w-12">
                    <span className="sr-only">Zdjęcie</span>
                  </Th>
                  <Th>Model</Th>
                  <Th>Kolor</Th>
                  <Th>Rozmiar</Th>
                  <Th>EAN</Th>
                  <Th>SKU</Th>
                  <Th numeric>Ilość</Th>
                  <Th numeric>Hurt netto</Th>
                  <Th numeric>MSRP brutto</Th>
                  <Th numeric>Wartość hurt.</Th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => {
                  const image = mediaUrl(item.imageStorageKey);
                  return (
                    <Tr key={item.position}>
                      <Td className="py-2">
                        <div className="relative aspect-[2/3] w-8 bg-well">
                          {image && <Image src={image} alt="" fill sizes="32px" className="object-cover" />}
                        </div>
                      </Td>
                      <Td className="whitespace-nowrap">
                        {item.productId ? (
                          <Link href={`/admin/products/${item.productId}`} className="font-medium underline-offset-4 hover:underline">
                            {item.productName}
                          </Link>
                        ) : (
                          <span className="font-medium">{item.productName}</span>
                        )}
                        <span className="tabular block text-meta text-ink-3">
                          {item.styleCode}
                          {!item.productId && " · model usunięty z katalogu"}
                        </span>
                      </Td>
                      <Td className="whitespace-nowrap text-ink-2">{item.colorName}</Td>
                      <Td className="tabular">{item.sizeCode}</Td>
                      <Td className="tabular select-all">{item.ean}</Td>
                      <Td className="tabular whitespace-nowrap text-ink-2">{item.sku}</Td>
                      <Td numeric>{item.quantity ?? <span className="text-ink-3">—</span>}</Td>
                      <Td numeric>{money(item.wholesalePrice)}</Td>
                      <Td numeric className="text-ink-2">
                        {money(item.retailPrice)}
                      </Td>
                      <Td numeric>
                        {item.quantity != null ? money(item.quantity * item.wholesalePrice) : <span className="text-ink-3">—</span>}
                      </Td>
                    </Tr>
                  );
                })}
              </tbody>
              <tfoot>
                <tr className="border-t border-foreground">
                  <Td colSpan={6} className="text-ink-3">
                    {withQuantity < items.length && withQuantity > 0
                      ? `Wartość liczona dla ${withQuantity} z ${items.length} pozycji z podaną ilością`
                      : "Razem"}
                  </Td>
                  <Td numeric className="font-medium">
                    {withQuantity > 0 ? units : "—"}
                  </Td>
                  <Td colSpan={2} />
                  <Td numeric className="font-medium">
                    {sheet.wholesaleTotal != null ? money(sheet.wholesaleTotal) : "—"}
                  </Td>
                </tr>
              </tfoot>
            </Table>
          </section>

        </div>
      </PageBody>
    </>
  );
}

function InfoBlock({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="border-t border-line pt-4">
      <h2 className="mb-3 text-meta text-ink-3">{title}</h2>
      {children}
    </section>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <>
      <dt className="text-ink-3">{label}</dt>
      <dd className="min-w-0">{children || <span className="text-ink-3">—</span>}</dd>
    </>
  );
}
