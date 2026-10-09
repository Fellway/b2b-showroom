"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/admin/ui/confirm-dialog";
import { Checkbox, Field, NumberInput, TextInput } from "@/components/admin/ui/field";
import { useFlash } from "@/components/admin/ui/flash";
import { PageBody, PageHeader } from "@/components/admin/ui/page-header";
import { EmptyState, ErrorState, LoadingRows } from "@/components/admin/ui/states";
import { Table, Td, Th, Tr } from "@/components/admin/ui/table";
import { adminApi, ApiError } from "@/lib/admin/api";
import { errorMessage } from "@/lib/admin/errors";
import {
  representativeError,
  representativesKey,
  useRepresentatives,
  type Representative,
  type RepresentativeInput,
} from "@/lib/admin/representatives";
import { FormSheet } from "@/components/admin/ui/form-sheet";
import { ActiveLabel } from "@/components/admin/ui/status-label";

const EMPTY: RepresentativeInput = { fullName: "", email: "", phone: "", active: true, sortOrder: 0 };

export function RepresentativesPage() {
  const { data, isPending, isError, error, refetch } = useRepresentatives();
  const [editing, setEditing] = useState<Representative | "new" | null>(null);

  const sorted = data ? [...data].sort((a, b) => a.sortOrder - b.sortOrder || a.fullName.localeCompare(b.fullName, "pl")) : [];
  const defaultId = sorted.find((rep) => rep.active)?.id;

  return (
    <>
      <PageHeader
        title="Opiekunowie handlowi"
        meta={data ? `${data.length}` : undefined}
        actions={
          <Button onClick={() => setEditing("new")}>
            <Plus aria-hidden strokeWidth={1.5} />
            Dodaj opiekuna
          </Button>
        }
      />
      <PageBody>
        <p className="mb-6 max-w-[70ch] text-ink-2">
          Pierwszy aktywny opiekun (najniższa kolejność) jest przypisywany do nowych zestawień i widoczny dla kupców w
          katalogu oraz w PDF.
        </p>
        {isPending ? (
          <LoadingRows rows={3} />
        ) : isError ? (
          <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
        ) : sorted.length === 0 ? (
          <EmptyState title="Brak opiekunów" action={<Button variant="secondary" onClick={() => setEditing("new")}>Dodaj opiekuna</Button>}>
            Bez opiekuna kupcy nie zobaczą w katalogu, z kim się kontaktować.
          </EmptyState>
        ) : (
          <Table>
            <thead>
              <tr>
                <Th>Imię i nazwisko</Th>
                <Th>E-mail</Th>
                <Th>Telefon</Th>
                <Th>Status</Th>
                <Th numeric>Kolejność</Th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((rep) => (
                <Tr key={rep.id} interactive onClick={() => setEditing(rep)}>
                  <Td>
                    <button type="button" className="text-left font-medium underline-offset-4 hover:underline" onClick={() => setEditing(rep)}>
                      {rep.fullName}
                    </button>
                    {rep.id === defaultId && <span className="block text-meta text-ink-3">Domyślny w katalogu i zestawieniach</span>}
                  </Td>
                  <Td className="break-all text-ink-2">{rep.email}</Td>
                  <Td className="tabular text-ink-2">{rep.phone ?? "—"}</Td>
                  <Td>
                    <ActiveLabel active={rep.active} />
                  </Td>
                  <Td numeric>{rep.sortOrder}</Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        )}
      </PageBody>
      <RepresentativeSheet
        key={editing === "new" ? "new" : editing?.id ?? "closed"}
        representative={editing}
        onClose={() => setEditing(null)}
      />
    </>
  );
}


function RepresentativeSheet({ representative, onClose }: { representative: Representative | "new" | null; onClose: () => void }) {
  const queryClient = useQueryClient();
  const flash = useFlash();
  const isNew = representative === "new";
  const initial: RepresentativeInput = representative && representative !== "new" ? { ...representative, phone: representative.phone ?? "" } : EMPTY;
  const [form, setForm] = useState<RepresentativeInput>(initial);
  const [error, setError] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const refresh = () => queryClient.invalidateQueries({ queryKey: representativesKey });

  const save = useMutation({
    mutationFn: () => {
      const body = { ...form, phone: form.phone?.trim() || null, fullName: form.fullName.trim(), email: form.email.trim() };
      return isNew
        ? adminApi.post("/sales-representatives", body)
        : adminApi.put(`/sales-representatives/${(representative as Representative).id}`, { ...body, version: (representative as Representative).version });
    },
    onSuccess: async () => {
      await refresh();
      flash(isNew ? "Dodano opiekuna" : "Zapisano zmiany");
      onClose();
    },
    onError: (e) => {
      setError(representativeError(e));
      if (e instanceof ApiError && e.status === 409) void refresh();
    },
  });

  const remove = useMutation({
    mutationFn: () => adminApi.delete(`/sales-representatives/${(representative as Representative).id}`),
    onSuccess: async () => {
      await refresh();
      flash("Usunięto opiekuna");
      setConfirmDelete(false);
      onClose();
    },
    onError: (e) => {
      setConfirmDelete(false);
      setError(representativeError(e));
    },
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    save.mutate();
  }

  return (
    <>
      <FormSheet
        open={representative !== null}
        onOpenChange={(open) => !open && onClose()}
        title={isNew ? "Nowy opiekun" : (representative as Representative | null)?.fullName ?? ""}
        onSubmit={submit}
        footer={
          <>
            <Button type="submit" disabled={save.isPending}>
              {save.isPending ? "Zapisywanie…" : isNew ? "Dodaj opiekuna" : "Zapisz zmiany"}
            </Button>
            <Button type="button" variant="text" onClick={onClose}>
              Anuluj
            </Button>
            {!isNew && (
              <Button type="button" variant="danger" className="ml-auto" onClick={() => setConfirmDelete(true)}>
                Usuń
              </Button>
            )}
          </>
        }
      >
        <Field label="Imię i nazwisko">
          <TextInput required maxLength={120} value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        </Field>
        <Field label="E-mail">
          <TextInput type="email" required maxLength={254} value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </Field>
        <Field label="Telefon" hint="Widoczny dla kupców w katalogu i w PDF.">
          <TextInput type="tel" maxLength={32} value={form.phone ?? ""} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        </Field>
        <Field label="Kolejność" hint="Niższa liczba = wyżej na liście; pierwszy aktywny jest domyślny.">
          <NumberInput
            type="number"
            min={0}
            step={1}
            value={form.sortOrder}
            onChange={(e) => setForm({ ...form, sortOrder: Math.max(0, Number.parseInt(e.target.value, 10) || 0) })}
            className="w-28"
          />
        </Field>
        <Checkbox label="Aktywny — widoczny dla kupców" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
        {error && (
          <p role="alert" className="text-danger">
            {error}
          </p>
        )}
      </FormSheet>
      {!isNew && representative && (
        <ConfirmDialog
          open={confirmDelete}
          onOpenChange={setConfirmDelete}
          title="Usunąć opiekuna?"
          confirmLabel="Usuń opiekuna"
          pending={remove.isPending}
          onConfirm={() => remove.mutate()}
        >
          {representative.fullName} zniknie z katalogu, a w panelu zestawienia stracą przypisanie. Pobrane już PDF-y
          zachowują dane kontaktowe z chwili utworzenia.
        </ConfirmDialog>
      )}
    </>
  );
}
