"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { ExternalLink, Mail, Phone, Search } from "lucide-react";
import { useState, type FormEvent, type ReactNode } from "react";
import { Pager } from "@/components/admin/line-sheets/pager";
import { ConfirmDialog } from "@/components/admin/ui/confirm-dialog";
import { Field, TextArea, TextInput } from "@/components/admin/ui/field";
import { useFlash } from "@/components/admin/ui/flash";
import { PageBody, PageHeader } from "@/components/admin/ui/page-header";
import { EmptyState, ErrorState, LoadingRows } from "@/components/admin/ui/states";
import { Table, Td, Th, Tr } from "@/components/admin/ui/table";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetDescription, SheetTitle } from "@/components/ui/sheet";
import {
  BUYER_FILTERS,
  BUYER_STATUS_LABEL,
  buyersKey,
  decide,
  deleteBuyer,
  useBuyer,
  useBuyers,
  type BuyerDecision,
  type BuyerDetail,
  type BuyerStatus,
} from "@/lib/admin/buyers";
import { errorMessage } from "@/lib/admin/errors";
import { formatDateTime, LANGUAGE_NAME } from "@/lib/admin/line-sheets";
import { cn } from "@/lib/utils";

const STATUS_VALUES = new Set<string>(BUYER_FILTERS.map((f) => f.value));
const countryName = (code: string) => new Intl.DisplayNames(["pl"], { type: "region" }).of(code) ?? code;

/**
 * Wnioski i konta kupców (D23). Domyślny widok to wnioski czekające na decyzję — po potwierdzeniu adresu e-mail
 * kupiec trafia tutaj, a zespół dostaje powiadomienie e-mailem z linkiem do tej strony.
 */
export function BuyersPage() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const statusParam = searchParams.get("status") ?? "PENDING_APPROVAL";
  const status = (STATUS_VALUES.has(statusParam) ? statusParam : "PENDING_APPROVAL") as BuyerStatus | "ALL";
  const search = searchParams.get("q") ?? "";
  const page = Math.max(0, Number.parseInt(searchParams.get("page") ?? "1", 10) - 1 || 0);
  const openId = searchParams.get("buyer");
  const { data, isPending, isError, error, refetch } = useBuyers(status, search, page);
  const [draft, setDraft] = useState(search);

  const navigate = (changes: Record<string, string | null>) => {
    const params = new URLSearchParams(searchParams);
    for (const [key, value] of Object.entries(changes)) {
      if (value === null || value === "") params.delete(key);
      else params.set(key, value);
    }
    const text = params.toString();
    router.replace(text ? `${pathname}?${text}` : pathname, { scroll: false });
  };

  function submitSearch(event: FormEvent) {
    event.preventDefault();
    navigate({ q: draft.trim(), page: null });
  }

  return (
    <>
      <PageHeader title="Kupcy" meta={data ? `${data.totalItems}` : undefined} />
      <PageBody>
        <div className="flex flex-wrap items-end justify-between gap-x-8 gap-y-4 pb-6">
          <nav aria-label="Status kont" className="flex flex-wrap gap-1">
            {BUYER_FILTERS.map((filter) => {
              const active = filter.value === status;
              return (
                <button
                  key={filter.value}
                  type="button"
                  aria-current={active ? "true" : undefined}
                  onClick={() => navigate({ status: filter.value === "PENDING_APPROVAL" ? null : filter.value, page: null })}
                  className={cn(
                    "h-9 px-3 transition-colors",
                    active ? "bg-foreground text-background" : "text-ink-2 hover:bg-well hover:text-foreground",
                  )}
                >
                  {filter.label}
                </button>
              );
            })}
          </nav>
          <form onSubmit={submitSearch} role="search" className="flex w-full max-w-xs items-center gap-2">
            <TextInput
              type="search"
              aria-label="Szukaj kupca"
              placeholder="Firma, e-mail, NIP…"
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              maxLength={100}
            />
            <Button type="submit" variant="secondary" aria-label="Szukaj">
              <Search aria-hidden strokeWidth={1.5} />
            </Button>
          </form>
        </div>

        {isPending ? (
          <LoadingRows />
        ) : isError ? (
          <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
        ) : data.items.length === 0 ? (
          <EmptyState title={search ? "Brak wyników" : status === "PENDING_APPROVAL" ? "Brak wniosków do akceptacji" : "Brak kont"}>
            {status === "PENDING_APPROVAL" && !search
              ? "Gdy kupiec zarejestruje się i potwierdzi adres e-mail, wniosek pojawi się tutaj, a zespół dostanie powiadomienie."
              : undefined}
          </EmptyState>
        ) : (
          <>
            <Table>
              <thead>
                <tr>
                  <Th>Firma</Th>
                  <Th>NIP / VAT</Th>
                  <Th>Osoba kontaktowa</Th>
                  <Th>Status</Th>
                  <Th>Rejestracja</Th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((buyer) => (
                  <Tr key={buyer.id} interactive onClick={() => navigate({ buyer: buyer.id })}>
                    <Td>
                      <button
                        type="button"
                        onClick={() => navigate({ buyer: buyer.id })}
                        className="text-left font-medium underline-offset-4 hover:underline"
                      >
                        {buyer.companyName}
                      </button>
                      <span className="block break-all text-meta text-ink-3">{buyer.email}</span>
                    </Td>
                    <Td className="tabular whitespace-nowrap">
                      {buyer.countryCode} {buyer.taxId}
                    </Td>
                    <Td>{buyer.contactName}</Td>
                    <Td>
                      <BuyerStatusLabel status={buyer.status} />
                    </Td>
                    <Td className="tabular whitespace-nowrap text-ink-2">{formatDateTime(buyer.createdAt)}</Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
            <Pager page={data.page} totalPages={data.totalPages} onChange={(next) => navigate({ page: next > 0 ? String(next + 1) : null })} />
          </>
        )}
      </PageBody>
      <BuyerSheet id={openId} onClose={() => navigate({ buyer: null })} />
    </>
  );
}

/** Ta sama gramatyka co status modelu: kropka pełna = może korzystać z cen, pusta = nie; tekst niesie znaczenie. */
function BuyerStatusLabel({ status }: { status: BuyerStatus }) {
  const active = status === "ACTIVE";
  const muted = status === "REJECTED" || status === "SUSPENDED";
  return (
    <span className={cn("inline-flex items-center gap-2 whitespace-nowrap", muted ? "text-ink-3" : active ? "text-foreground" : "text-ink-2")}>
      <span
        aria-hidden
        className={cn("size-2 rounded-full border", active ? "border-foreground bg-foreground" : muted ? "border-ink-3" : "border-foreground")}
      />
      {BUYER_STATUS_LABEL[status]}
    </span>
  );
}

function BuyerSheet({ id, onClose }: { id: string | null; onClose: () => void }) {
  const { data: buyer, isPending, isError, error } = useBuyer(id);
  return (
    <Sheet open={id !== null} onOpenChange={(open) => !open && onClose()}>
      <SheetContent
        side="right"
        closeLabel="Zamknij"
        className="w-full gap-0 border-line p-0 shadow-panel data-[side=right]:w-full data-[side=right]:sm:max-w-[560px]"
      >
        <div className="flex h-(--header-height) shrink-0 items-center border-b border-line px-6 pr-14">
          <SheetTitle className="truncate text-body font-medium">{buyer?.companyName ?? "Kupiec"}</SheetTitle>
        </div>
        <SheetDescription className="sr-only">Dane konta kupca i decyzja o dostępie do cen</SheetDescription>
        {id === null ? null : isPending ? (
          <div className="px-6 py-5">
            <LoadingRows rows={4} />
          </div>
        ) : isError ? (
          <div className="px-6 py-5">
            <ErrorState message={errorMessage(error)} />
          </div>
        ) : (
          <BuyerDetails key={`${buyer.id}:${buyer.version}`} buyer={buyer} onDeleted={onClose} />
        )}
      </SheetContent>
    </Sheet>
  );
}

const DECISION_FLASH: Record<BuyerDecision, string> = {
  approve: "Konto zaakceptowane — kupiec dostał e-mail.",
  reject: "Wniosek odrzucony — kupiec dostał e-mail.",
  suspend: "Konto zawieszone — sesje kupca zakończone.",
  reactivate: "Konto przywrócone.",
};

function BuyerDetails({ buyer, onDeleted }: { buyer: BuyerDetail; onDeleted: () => void }) {
  const queryClient = useQueryClient();
  const flash = useFlash();
  const [note, setNote] = useState("");
  const [confirmDelete, setConfirmDelete] = useState(false);

  const decision = useMutation({
    mutationFn: (kind: BuyerDecision) => decide(buyer.id, kind, buyer.version, note),
    onSuccess: (_, kind) => {
      flash(DECISION_FLASH[kind]);
      setNote("");
      void queryClient.invalidateQueries({ queryKey: buyersKey });
    },
  });
  const removal = useMutation({
    mutationFn: () => deleteBuyer(buyer.id),
    onSuccess: () => {
      flash("Konto usunięte.");
      setConfirmDelete(false);
      onDeleted();
      void queryClient.invalidateQueries({ queryKey: buyersKey });
    },
  });

  const takesNote = buyer.status === "PENDING_APPROVAL" || buyer.status === "ACTIVE";
  const pending = decision.isPending;

  return (
    <>
      <div className="min-h-0 flex-1 overflow-y-auto px-6 py-5">
        <div className="flex flex-col gap-6">
          <div>
            <BuyerStatusLabel status={buyer.status} />
            {buyer.status === "PENDING_EMAIL" && (
              <p className="mt-2 text-small text-ink-2">
                Kupiec nie potwierdził jeszcze adresu e-mail. Wniosek będzie można rozpatrzyć po potwierdzeniu.
              </p>
            )}
          </div>

          <Block title="Firma">
            <Row label="Nazwa">{buyer.companyName}</Row>
            <Row label="Kraj">{countryName(buyer.countryCode)}</Row>
            <Row label="NIP / VAT">
              <span className="tabular select-all">
                {buyer.countryCode} {buyer.taxId}
              </span>
            </Row>
          </Block>
          <p className="-mt-3 flex flex-wrap gap-x-4 gap-y-1 text-meta text-ink-3">
            Sprawdź w rejestrze:
            <a href="https://ec.europa.eu/taxation_customs/vies/" target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 underline underline-offset-4 hover:text-foreground">
              VIES (UE) <ExternalLink aria-hidden className="size-3" strokeWidth={1.5} />
            </a>
            {buyer.countryCode === "PL" && (
              <a href="https://www.podatki.gov.pl/wykaz-podatnikow-vat-wyszukiwarka" target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 underline underline-offset-4 hover:text-foreground">
                Biała lista VAT <ExternalLink aria-hidden className="size-3" strokeWidth={1.5} />
              </a>
            )}
          </p>

          <Block title="Kontakt">
            <Row label="Osoba">{buyer.contactName}</Row>
            <Row label="E-mail">
              <a href={`mailto:${buyer.email}`} className="inline-flex items-center gap-1.5 break-all underline-offset-4 hover:underline">
                <Mail aria-hidden className="size-3.5 shrink-0 text-ink-3" strokeWidth={1.5} />
                {buyer.email}
              </a>
            </Row>
            <Row label="Telefon">
              <a href={`tel:${buyer.phone.replace(/[^\d+]/g, "")}`} className="tabular inline-flex items-center gap-1.5 underline-offset-4 hover:underline">
                <Phone aria-hidden className="size-3.5 shrink-0 text-ink-3" strokeWidth={1.5} />
                {buyer.phone}
              </a>
            </Row>
            <Row label="Język">{LANGUAGE_NAME[buyer.language] ?? buyer.language}</Row>
          </Block>

          <Block title="Historia">
            <Row label="Rejestracja">{formatDateTime(buyer.createdAt)}</Row>
            <Row label="Potwierdzenie e-mail">{formatDateTime(buyer.emailVerifiedAt)}</Row>
            <Row label="Ostatnia decyzja">{formatDateTime(buyer.reviewedAt)}</Row>
            {buyer.reviewNote && <Row label="Notatka">{buyer.reviewNote}</Row>}
            <Row label="Ostatnie logowanie">{buyer.lastLoginAt ? formatDateTime(buyer.lastLoginAt) : "Nigdy"}</Row>
          </Block>

          <Block title="Zgody i dokumenty">
            {buyer.consents.length === 0 ? (
              <Row label="Rejestr">Brak — kupiec zaakceptuje regulamin przy logowaniu</Row>
            ) : (
              buyer.consents.map((consent) => (
                <Row
                  key={`${consent.documentVersionId}-${consent.acceptedAt}`}
                  label={`${consent.documentType === "TERMS" ? "Regulamin" : "Polityka prywatności"} v${consent.versionNumber}`}
                >
                  <span className="tabular">{formatDateTime(consent.acceptedAt)}</span>
                  <span className="text-ink-3">
                    {" "}
                    · {consent.context === "REGISTRATION" ? "rejestracja" : "nowa wersja"} · {consent.language.toLowerCase()}
                  </span>
                </Row>
              ))
            )}
          </Block>

          {takesNote && (
            <Field label="Notatka wewnętrzna" hint="Zapisywana przy odrzuceniu lub zawieszeniu. Kupiec jej nie zobaczy.">
              <TextArea rows={3} maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} />
            </Field>
          )}
        </div>
      </div>

      <div className="flex shrink-0 flex-col gap-3 border-t border-line px-6 py-4">
        {(decision.error || removal.error) && (
          <p role="alert" className="text-small text-danger">
            {errorMessage(decision.error ?? removal.error)}
          </p>
        )}
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="danger" onClick={() => setConfirmDelete(true)}>
            Usuń konto
          </Button>
          <div className="ml-auto flex flex-wrap items-center gap-2">
            {buyer.status === "PENDING_APPROVAL" && (
              <>
                <Button variant="secondary" size="lg" disabled={pending} onClick={() => decision.mutate("reject")}>
                  Odrzuć
                </Button>
                <Button size="lg" disabled={pending} onClick={() => decision.mutate("approve")}>
                  Zaakceptuj
                </Button>
              </>
            )}
            {buyer.status === "REJECTED" && (
              <Button size="lg" disabled={pending} onClick={() => decision.mutate("approve")}>
                Zaakceptuj mimo wszystko
              </Button>
            )}
            {buyer.status === "ACTIVE" && (
              <Button variant="secondary" size="lg" disabled={pending} onClick={() => decision.mutate("suspend")}>
                Zawieś dostęp
              </Button>
            )}
            {buyer.status === "SUSPENDED" && (
              <Button size="lg" disabled={pending} onClick={() => decision.mutate("reactivate")}>
                Przywróć dostęp
              </Button>
            )}
          </div>
        </div>
      </div>

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title={`Usunąć konto ${buyer.companyName}?`}
        confirmLabel="Usuń konto"
        onConfirm={() => removal.mutate()}
        pending={removal.isPending}
      >
        Konto i dane logowania zostaną trwale usunięte (np. na prośbę kupca — RODO). Wygenerowane zestawienia zostaną
        w historii jako migawki. Tej operacji nie można cofnąć.
      </ConfirmDialog>
    </>
  );
}

function Block({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-2 border-t border-line pt-4">
      <h3 className="text-small font-medium">{title}</h3>
      <dl className="grid grid-cols-[minmax(0,10rem)_1fr] gap-x-4 gap-y-1.5">{children}</dl>
    </section>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <>
      <dt className="text-ink-3">{label}</dt>
      <dd className="min-w-0">{children}</dd>
    </>
  );
}
