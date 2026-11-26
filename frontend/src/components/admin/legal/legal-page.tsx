"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ExternalLink } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { ConfirmDialog } from "@/components/admin/ui/confirm-dialog";
import { Checkbox, Field, TextArea, TextInput } from "@/components/admin/ui/field";
import { useFlash } from "@/components/admin/ui/flash";
import { PageBody, PageHeader } from "@/components/admin/ui/page-header";
import { ErrorState, LoadingRows } from "@/components/admin/ui/states";
import { Table, Td, Th, Tr } from "@/components/admin/ui/table";
import { LegalMarkdown } from "@/components/legal/legal-markdown";
import { useSession } from "@/lib/admin/use-session";
import {
  createDraft,
  deleteDraft,
  LEGAL_LANGUAGES,
  LEGAL_SLUG,
  LEGAL_TYPE_LABEL,
  legalError,
  legalKey,
  publishDraft,
  saveDraft,
  useLegalOverview,
  useLegalVersion,
  type LegalLanguage,
  type LegalOverview,
  type LegalText,
  type LegalVersion,
} from "@/lib/admin/legal";
import { cn } from "@/lib/utils";

const dateTime = (iso: string | null) =>
  iso ? new Intl.DateTimeFormat("pl", { dateStyle: "medium", timeStyle: "short" }).format(new Date(iso)) : "—";

/**
 * Dokumenty prawne (D25): obowiązująca wersja, historia i szkic następnej wersji. Opublikowana wersja jest
 * niezmienna — rejestr zgód kupców wskazuje konkretną wersję. Szkic edytuje ADMIN i EDITOR, publikuje ADMIN.
 */
export function LegalPage() {
  // edytowany szkic w adresie (?edit=<id>) — działa powrót w nagłówku i przycisk Wstecz przeglądarki
  const router = useRouter();
  const editing = useSearchParams().get("edit");
  return editing ? (
    <DraftEditor id={editing} onClose={() => router.push("/admin/legal")} />
  ) : (
    <Overview onEdit={(id) => router.push(`/admin/legal?edit=${encodeURIComponent(id)}`)} />
  );
}

function Overview({ onEdit }: { onEdit: (id: string) => void }) {
  const { data, isPending, isError, error, refetch } = useLegalOverview();
  return (
    <>
      <PageHeader title="Dokumenty prawne" />
      <PageBody>
        <p className="mb-8 max-w-[72ch] text-ink-2">
          Regulamin i polityka prywatności w czterech językach. Kupcy akceptują konkretną wersję — każda zmiana to nowa
          wersja, a poprzednie zostają w historii. Nowa wersja regulaminu oznaczona jako istotna blokuje kupcom ceny
          i zestawienia, dopóki jej nie zaakceptują; o nowej polityce prywatności kupcy są tylko informowani.
        </p>
        {isPending ? (
          <LoadingRows rows={4} />
        ) : isError ? (
          <ErrorState message={legalError(error)} onRetry={() => void refetch()} />
        ) : (
          <div className="grid gap-12">
            {data.map((document) => (
              <DocumentBlock key={document.type} document={document} onEdit={onEdit} />
            ))}
          </div>
        )}
      </PageBody>
    </>
  );
}

function DocumentBlock({ document, onEdit }: { document: LegalOverview; onEdit: (id: string) => void }) {
  const queryClient = useQueryClient();
  const create = useMutation({
    mutationFn: () => createDraft(document.type),
    onSuccess: async (draft) => {
      await queryClient.invalidateQueries({ queryKey: legalKey });
      onEdit(draft.id);
    },
  });
  const slug = LEGAL_SLUG[document.type];

  return (
    <section aria-labelledby={`legal-${slug}`}>
      <div className="flex flex-wrap items-baseline justify-between gap-4 border-b border-line pb-3">
        <div>
          <h2 id={`legal-${slug}`} className="text-body font-medium">
            {LEGAL_TYPE_LABEL[document.type]}
          </h2>
          <p className="mt-0.5 text-small text-ink-3">
            {document.current
              ? `Obowiązuje wersja ${document.current.versionNumber} z ${dateTime(document.current.publishedAt)}`
              : "Brak opublikowanej wersji"}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <a
            href={`/${slug}`}
            target="_blank"
            rel="noopener"
            className="inline-flex h-10 items-center gap-1.5 px-3 text-small text-ink-2 hover:text-foreground"
          >
            Strona dokumentu <ExternalLink aria-hidden className="size-3.5" strokeWidth={1.5} />
          </a>
          {document.draft ? (
            <Button onClick={() => onEdit(document.draft!.id)}>Edytuj szkic nowej wersji</Button>
          ) : (
            <Button variant="secondary" onClick={() => create.mutate()} disabled={create.isPending}>
              {create.isPending ? "Tworzenie…" : "Przygotuj nową wersję"}
            </Button>
          )}
        </div>
      </div>
      {create.error && (
        <p role="alert" className="mt-3 text-small text-danger">
          {legalError(create.error)}
        </p>
      )}
      {document.draft && (
        <p className="mt-3 text-small text-ink-2">
          Szkic utworzony {dateTime(document.draft.createdAt)} — niewidoczny dla kupców do czasu publikacji.
        </p>
      )}
      {document.published.length > 0 && (
        <Table className="mt-4">
          <thead>
            <tr>
              <Th>Wersja</Th>
              <Th>Opublikowana</Th>
              <Th>Akceptacja kupców</Th>
              <Th>Treść</Th>
            </tr>
          </thead>
          <tbody>
            {document.published.map((version) => (
              <Tr key={version.id}>
                <Td className="tabular font-medium">
                  {version.versionNumber}
                  {version.id === document.current?.id && <span className="ml-2 font-normal text-ink-3">obowiązująca</span>}
                </Td>
                <Td className="tabular text-ink-2">{dateTime(version.publishedAt)}</Td>
                <Td className="text-ink-2">
                  {document.type === "TERMS"
                    ? version.acceptanceRequired
                      ? "wymagana"
                      : "poprawka redakcyjna — bez ponownej akceptacji"
                    : "informacja dla kupców"}
                </Td>
                <Td>
                  <a
                    href={`/${slug}?version=${version.id}`}
                    target="_blank"
                    rel="noopener"
                    className="text-small underline-offset-4 hover:underline"
                  >
                    Zobacz
                  </a>
                </Td>
              </Tr>
            ))}
          </tbody>
        </Table>
      )}
    </section>
  );
}

const EMPTY_TEXT: LegalText = { title: "", body: "", changeSummary: null };

function DraftEditor({ id, onClose }: { id: string; onClose: () => void }) {
  const { data, isPending, isError, error, refetch } = useLegalVersion(id);
  if (isPending) {
    return (
      <>
        <PageHeader title="Szkic dokumentu" back={{ href: "/admin/legal", label: "Dokumenty prawne" }} />
        <PageBody>
          <LoadingRows rows={6} />
        </PageBody>
      </>
    );
  }
  if (isError) {
    return (
      <>
        <PageHeader title="Szkic dokumentu" back={{ href: "/admin/legal", label: "Dokumenty prawne" }} />
        <PageBody>
          <ErrorState message={legalError(error)} onRetry={() => void refetch()} />
        </PageBody>
      </>
    );
  }
  return <DraftForm key={`${data.id}-${data.version}`} draft={data} onClose={onClose} />;
}

function DraftForm({ draft, onClose }: { draft: LegalVersion; onClose: () => void }) {
  const queryClient = useQueryClient();
  const flash = useFlash();
  const session = useSession();
  const isAdmin = session.status === "authenticated" && session.admin.role === "ADMIN";
  const [texts, setTexts] = useState<Record<LegalLanguage, LegalText>>(() => ({
    pl: draft.texts.pl ?? EMPTY_TEXT,
    en: draft.texts.en ?? EMPTY_TEXT,
    cs: draft.texts.cs ?? EMPTY_TEXT,
    uk: draft.texts.uk ?? EMPTY_TEXT,
  }));
  const [language, setLanguage] = useState<LegalLanguage>("pl");
  const [preview, setPreview] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [version, setVersion] = useState(draft.version);
  const [error, setError] = useState<string | null>(null);
  const [publishOpen, setPublishOpen] = useState(false);
  const [requireAcceptance, setRequireAcceptance] = useState(true);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const isTerms = draft.type === "TERMS";
  const text = texts[language];
  const complete = (code: LegalLanguage) => texts[code].title.trim() !== "" && texts[code].body.trim() !== "";
  const missing = LEGAL_LANGUAGES.filter((l) => !complete(l.code));

  const update = (patch: Partial<LegalText>) => {
    setTexts((current) => ({ ...current, [language]: { ...current[language], ...patch } }));
    setDirty(true);
  };

  const refresh = () => queryClient.invalidateQueries({ queryKey: legalKey });

  async function persist(): Promise<number> {
    if (!dirty) return version;
    const body = Object.fromEntries(
      LEGAL_LANGUAGES.map(({ code }) => [
        code,
        {
          title: texts[code].title.trim(),
          body: texts[code].body,
          changeSummary: texts[code].changeSummary?.trim() || null,
        },
      ]),
    );
    const saved = await saveDraft(draft.id, version, body);
    setVersion(saved.version);
    setDirty(false);
    return saved.version;
  }

  const save = useMutation({
    mutationFn: persist,
    onSuccess: () => {
      flash("Zapisano szkic");
      void refresh();
    },
    onError: (e) => setError(legalError(e)),
  });

  const publish = useMutation({
    mutationFn: async () => publishDraft(draft.id, await persist(), isTerms && requireAcceptance),
    onSuccess: async () => {
      await refresh();
      flash(`Opublikowano nową wersję: ${LEGAL_TYPE_LABEL[draft.type].toLowerCase()}`);
      onClose();
    },
    onError: (e) => {
      setPublishOpen(false);
      setError(legalError(e));
    },
  });

  const remove = useMutation({
    mutationFn: () => deleteDraft(draft.id),
    onSuccess: async () => {
      await refresh();
      flash("Usunięto szkic");
      onClose();
    },
    onError: (e) => {
      setConfirmDelete(false);
      setError(legalError(e));
    },
  });

  return (
    <>
      <PageHeader
        title={`${LEGAL_TYPE_LABEL[draft.type]} — szkic nowej wersji`}
        meta={dirty ? "Niezapisane zmiany" : undefined}
        back={{ href: "/admin/legal", label: "Dokumenty prawne" }}
        actions={
          <>
            <Button variant="text" onClick={onClose}>
              Wróć
            </Button>
            <Button variant="secondary" onClick={() => { setError(null); save.mutate(); }} disabled={save.isPending || !dirty}>
              {save.isPending ? "Zapisywanie…" : "Zapisz szkic"}
            </Button>
            <Button
              onClick={() => { setError(null); setPublishOpen(true); }}
              disabled={!isAdmin || missing.length > 0}
              title={!isAdmin ? "Publikuje administrator" : missing.length > 0 ? "Uzupełnij wszystkie języki" : undefined}
            >
              Opublikuj…
            </Button>
          </>
        }
      />
      <PageBody>
        <div className="mb-6 max-w-[80ch] text-small text-ink-2">
          Treść w prostym formacie Markdown: <code className="bg-well px-1">## Nagłówek</code>,{" "}
          <code className="bg-well px-1">- punkt</code>, <code className="bg-well px-1">1. punkt</code>,{" "}
          <code className="bg-well px-1">**pogrubienie**</code>, <code className="bg-well px-1">[link](https://…)</code>,{" "}
          <code className="bg-well px-1">&gt; uwaga</code>. Przed publikacją treść powinien zatwierdzić prawnik marki.
          {!isAdmin && " Szkic opublikuje administrator."}
        </div>

        <div role="tablist" aria-label="Język" className="flex flex-wrap gap-x-6 border-b border-line">
          {LEGAL_LANGUAGES.map(({ code, label }) => (
            <button
              key={code}
              type="button"
              role="tab"
              aria-selected={language === code}
              onClick={() => setLanguage(code)}
              className={cn(
                "-mb-px border-b-2 py-2.5 text-small",
                language === code ? "border-foreground font-medium" : "border-transparent text-ink-2 hover:text-foreground",
              )}
            >
              {label}
              {!complete(code) && <span className="ml-1.5 text-danger">· do uzupełnienia</span>}
            </button>
          ))}
        </div>

        <div className="mt-6 grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] xl:gap-10">
          <div className="grid content-start gap-5">
            <Field label="Tytuł">
              <TextInput maxLength={200} value={text.title} onChange={(e) => update({ title: e.target.value })} />
            </Field>
            <Field label="Co się zmieniło" hint="Krótko, dla kupców — pokazywane przy akceptacji nowej wersji. Opcjonalne.">
              <TextArea rows={2} maxLength={1000} value={text.changeSummary ?? ""} onChange={(e) => update({ changeSummary: e.target.value })} />
            </Field>
            <Field
              label="Treść"
              aside={
                <button type="button" onClick={() => setPreview((p) => !p)} className="text-meta text-ink-2 underline-offset-4 hover:underline xl:hidden">
                  {preview ? "Edycja" : "Podgląd"}
                </button>
              }
            >
              <TextArea
                rows={28}
                maxLength={100_000}
                value={text.body}
                onChange={(e) => update({ body: e.target.value })}
                className={cn("font-mono text-meta", preview && "hidden xl:block")}
              />
            </Field>
          </div>
          <div className={cn("border-t border-line pt-5 xl:border-l xl:border-t-0 xl:pl-10 xl:pt-0", !preview && "hidden xl:block")}>
            <p className="text-meta text-ink-3">Podgląd</p>
            <h2 className="mt-3 text-title font-medium">{text.title || "—"}</h2>
            <LegalMarkdown source={text.body} className="mt-6 text-small" />
          </div>
        </div>

        {error && (
          <p role="alert" className="mt-6 text-small text-danger">
            {error}
          </p>
        )}
        <div className="mt-10 border-t border-line pt-5">
          <Button variant="danger" onClick={() => setConfirmDelete(true)}>
            Usuń szkic
          </Button>
        </div>
      </PageBody>

      <Dialog open={publishOpen} onOpenChange={setPublishOpen}>
        <DialogContent className="max-w-md gap-0 rounded-none border-line p-6 shadow-menu">
          <DialogTitle className="text-body font-medium">Opublikować nową wersję?</DialogTitle>
          <DialogDescription asChild>
            <div className="mt-2 grid gap-3 text-small text-ink-2">
              <p>
                Wersja zacznie obowiązywać od razu we wszystkich językach. Po publikacji treści nie można już zmienić —
                kolejna zmiana to kolejna wersja.
              </p>
              {isTerms ? (
                <Checkbox
                  label="Zmiana istotna — kupcy muszą ją zaakceptować przed dostępem do cen i zestawień"
                  checked={requireAcceptance}
                  onChange={(e) => setRequireAcceptance(e.target.checked)}
                />
              ) : (
                <p>Zalogowani kupcy zobaczą informację o nowej wersji polityki prywatności.</p>
              )}
              {isTerms && !requireAcceptance && (
                <p className="text-meta text-ink-3">Bez zaznaczenia: poprawka redakcyjna, kupcy nie są o nic pytani.</p>
              )}
            </div>
          </DialogDescription>
          <div className="mt-6 flex justify-end gap-2">
            <Button variant="text" onClick={() => setPublishOpen(false)}>
              Anuluj
            </Button>
            <Button onClick={() => publish.mutate()} disabled={publish.isPending}>
              {publish.isPending ? "Publikowanie…" : "Opublikuj"}
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="Usunąć szkic?"
        confirmLabel="Usuń szkic"
        pending={remove.isPending}
        onConfirm={() => remove.mutate()}
      >
        Niezapisane i zapisane zmiany w szkicu przepadną. Obowiązująca wersja dokumentu się nie zmieni.
      </ConfirmDialog>
    </>
  );
}
