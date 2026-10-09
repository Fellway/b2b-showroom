"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Check, Trash2 } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/admin/ui/confirm-dialog";
import { useFlash } from "@/components/admin/ui/flash";
import { PageBody, PageHeader } from "@/components/admin/ui/page-header";
import { ErrorState, LoadingRows } from "@/components/admin/ui/states";
import { Table, Td, Th, Tr } from "@/components/admin/ui/table";
import { adminApi, ApiError } from "@/lib/admin/api";
import { dictionaryKeys, useSizes, type AdminSize } from "@/lib/admin/dictionaries";
import { errorMessage } from "@/lib/admin/errors";
import { parseOrder } from "./helpers";

const cellInput =
  "tabular h-9 w-full border border-edge bg-background px-2 text-small outline-none transition-colors hover:border-foreground focus-visible:border-foreground focus-visible:outline-none aria-invalid:border-danger";

/** Rozmiarówka: krótka lista edytowana w wierszach — kolejność wyznacza porządek rozmiarów w katalogu i PDF. */
export function SizesView() {
  const sizes = useSizes();
  const sorted = [...(sizes.data ?? [])].sort((a, b) => a.sortOrder - b.sortOrder);
  const nextOrder = sorted.length ? sorted[sorted.length - 1].sortOrder + 10 : 10;

  return (
    <>
      <PageHeader title="Rozmiary" meta={sizes.data ? <span className="tabular">{sizes.data.length}</span> : undefined} />
      <PageBody narrow>
        <p className="mb-6 max-w-[60ch] text-ink-2">
          Kolejność ustala porządek rozmiarów na karcie produktu, w filtrach i w zestawieniu PDF. Rozmiaru użytego w wariantach nie można usunąć.
        </p>
        {sizes.isPending ? (
          <LoadingRows rows={6} />
        ) : sizes.isError ? (
          <ErrorState message={errorMessage(sizes.error)} onRetry={() => sizes.refetch()} />
        ) : (
          <Table>
            <thead>
              <tr>
                <Th className="w-40">Rozmiar</Th>
                <Th className="w-32">Kolejność</Th>
                <Th numeric>Warianty</Th>
                <Th>
                  <span className="sr-only">Akcje</span>
                </Th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((size) => (
                <SizeRow key={`${size.id}:${size.version}`} size={size} />
              ))}
              <NewSizeRow key={sorted.length} suggestedOrder={nextOrder} />
            </tbody>
          </Table>
        )}
      </PageBody>
    </>
  );
}

function SizeRow({ size }: { size: AdminSize }) {
  const client = useQueryClient();
  const flash = useFlash();
  const [code, setCode] = useState(size.code);
  const [order, setOrder] = useState(String(size.sortOrder));
  const [error, setError] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const dirty = code !== size.code || order !== String(size.sortOrder);

  const save = useMutation({
    mutationFn: (body: Record<string, unknown>) => adminApi.put(`/sizes/${size.id}`, body),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: dictionaryKeys.sizes });
      flash(`Zapisano rozmiar ${code.trim().toUpperCase()}`);
    },
    onError: (e) => {
      setError(errorMessage(e));
      if (e instanceof ApiError && e.code === "data.concurrent-modification") client.invalidateQueries({ queryKey: dictionaryKeys.sizes });
    },
  });

  const remove = useMutation({
    mutationFn: () => adminApi.delete(`/sizes/${size.id}`),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: dictionaryKeys.sizes });
      flash(`Usunięto rozmiar ${size.code}`);
      setConfirmDelete(false);
    },
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const sortOrder = parseOrder(order);
    if (!code.trim() || sortOrder === null) {
      setError(!code.trim() ? "Podaj oznaczenie rozmiaru." : "Kolejność: liczba całkowita ≥ 0.");
      return;
    }
    setError(null);
    save.mutate({ code: code.trim(), sortOrder, version: size.version });
  };

  const formId = `size-${size.id}`;
  return (
    <Tr>
      <Td>
        <form id={formId} onSubmit={submit} />
        <input
          form={formId}
          aria-label={`Oznaczenie rozmiaru ${size.code}`}
          value={code}
          maxLength={16}
          onChange={(e) => setCode(e.target.value.toUpperCase())}
          className={cellInput}
        />
      </Td>
      <Td>
        <input
          form={formId}
          inputMode="numeric"
          aria-label={`Kolejność rozmiaru ${size.code}`}
          value={order}
          onChange={(e) => setOrder(e.target.value)}
          className={`${cellInput} text-right`}
        />
      </Td>
      <Td numeric className="text-ink-2">
        {size.variantCount}
      </Td>
      <Td className="text-right">
        <div className="flex items-center justify-end gap-1">
          {error && (
            <span role="alert" className="mr-2 text-meta text-danger">
              {error}
            </span>
          )}
          {dirty && (
            <Button form={formId} type="submit" variant="secondary" size="sm" disabled={save.isPending}>
              <Check aria-hidden strokeWidth={1.5} />
              {save.isPending ? "Zapisywanie…" : "Zapisz"}
            </Button>
          )}
          <Button
            type="button"
            variant="icon"
            size="icon-sm"
            aria-label={`Usuń rozmiar ${size.code}`}
            disabled={size.variantCount > 0}
            title={size.variantCount > 0 ? "Rozmiar jest używany w wariantach" : undefined}
            onClick={() => setConfirmDelete(true)}
          >
            <Trash2 aria-hidden strokeWidth={1.5} />
          </Button>
        </div>
        <ConfirmDialog
          open={confirmDelete}
          onOpenChange={(open) => {
            setConfirmDelete(open);
            if (!open) remove.reset();
          }}
          title={`Usunąć rozmiar ${size.code}?`}
          confirmLabel="Usuń rozmiar"
          onConfirm={() => remove.mutate()}
          pending={remove.isPending}
        >
          {remove.isError ? <span className="text-danger">{errorMessage(remove.error)}</span> : "Tej operacji nie można cofnąć."}
        </ConfirmDialog>
      </Td>
    </Tr>
  );
}

function NewSizeRow({ suggestedOrder }: { suggestedOrder: number }) {
  const client = useQueryClient();
  const flash = useFlash();
  const [code, setCode] = useState("");
  const [order, setOrder] = useState(String(suggestedOrder));
  const [error, setError] = useState<string | null>(null);

  const create = useMutation({
    mutationFn: (body: Record<string, unknown>) => adminApi.post<{ id: string }>("/sizes", body),
    onSuccess: (_, body) => {
      client.invalidateQueries({ queryKey: dictionaryKeys.sizes });
      flash(`Dodano rozmiar ${String(body.code)}`);
    },
    onError: (e) => setError(errorMessage(e)),
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const sortOrder = parseOrder(order);
    if (!code.trim() || sortOrder === null) {
      setError(!code.trim() ? "Podaj oznaczenie rozmiaru, np. 48 lub XL." : "Kolejność: liczba całkowita ≥ 0.");
      return;
    }
    setError(null);
    create.mutate({ code: code.trim().toUpperCase(), sortOrder });
  };

  return (
    <Tr className="bg-well/50">
      <Td>
        <form id="size-new" onSubmit={submit} />
        <input
          form="size-new"
          aria-label="Nowy rozmiar"
          placeholder="Nowy, np. 48"
          value={code}
          maxLength={16}
          onChange={(e) => setCode(e.target.value.toUpperCase())}
          className={cellInput}
        />
      </Td>
      <Td>
        <input
          form="size-new"
          inputMode="numeric"
          aria-label="Kolejność nowego rozmiaru"
          value={order}
          onChange={(e) => setOrder(e.target.value)}
          className={`${cellInput} text-right`}
        />
      </Td>
      <Td />
      <Td className="text-right">
        <div className="flex items-center justify-end gap-2">
          {error && (
            <span role="alert" className="text-meta text-danger">
              {error}
            </span>
          )}
          <Button form="size-new" type="submit" variant="secondary" size="sm" disabled={create.isPending}>
            {create.isPending ? "Dodawanie…" : "Dodaj rozmiar"}
          </Button>
        </div>
      </Td>
    </Tr>
  );
}
