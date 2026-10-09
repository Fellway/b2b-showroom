"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/admin/ui/confirm-dialog";
import { Field, NumberInput, TextInput } from "@/components/admin/ui/field";
import { useFlash } from "@/components/admin/ui/flash";
import { PageBody, PageHeader } from "@/components/admin/ui/page-header";
import { EmptyState, ErrorState, LoadingRows } from "@/components/admin/ui/states";
import { Table, Td, Th, Tr } from "@/components/admin/ui/table";
import { adminApi, ApiError } from "@/lib/admin/api";
import { colorName, dictionaryKeys, LANGUAGE_LABEL, LANGUAGES, useColors, type AdminColor, type Language } from "@/lib/admin/dictionaries";
import { errorMessage } from "@/lib/admin/errors";
import { FormSheet, SheetActions } from "@/components/admin/ui/form-sheet";
import { missingLanguages, missingText, parseOrder } from "./helpers";

const HEX = /^#[0-9a-fA-F]{6}$/;

export function ColorsView() {
  const colors = useColors();
  const [editing, setEditing] = useState<AdminColor | "new" | null>(null);
  const sorted = [...(colors.data ?? [])].sort((a, b) => a.sortOrder - b.sortOrder || a.code.localeCompare(b.code));

  return (
    <>
      <PageHeader
        title="Kolory"
        meta={colors.data ? <span className="tabular">{colors.data.length}</span> : undefined}
        actions={
          <Button onClick={() => setEditing("new")}>
            <Plus aria-hidden strokeWidth={1.5} />
            Nowy kolor
          </Button>
        }
      />
      <PageBody>
        {colors.isPending ? (
          <LoadingRows rows={5} />
        ) : colors.isError ? (
          <ErrorState message={errorMessage(colors.error)} onRetry={() => colors.refetch()} />
        ) : sorted.length === 0 ? (
          <EmptyState title="Nie ma jeszcze kolorów.">Kolory to słownik wspólny dla modeli — nazwij je tak, jak widzi je kupiec, np. „Granat w kwiaty”.</EmptyState>
        ) : (
          <Table>
            <thead>
              <tr>
                <Th className="w-10">
                  <span className="sr-only">Próbka</span>
                </Th>
                <Th>Nazwa</Th>
                <Th>Kod</Th>
                <Th>Tłumaczenia</Th>
                <Th>HEX</Th>
                <Th numeric>Modele</Th>
                <Th numeric>Kolejność</Th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((color) => (
                <Tr key={color.id} interactive onClick={() => setEditing(color)}>
                  <Td>
                    <span
                      aria-hidden
                      className="block size-5 border border-black/15"
                      style={{ backgroundColor: color.hex ?? "transparent" }}
                    />
                  </Td>
                  <Td>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setEditing(color);
                      }}
                      className="text-left font-medium underline-offset-4 hover:underline"
                    >
                      {colorName(color)}
                    </button>
                  </Td>
                  <Td className="tabular text-ink-2">{color.code}</Td>
                  <Td className="text-meta text-ink-3">{missingText(missingLanguages(color.names))}</Td>
                  <Td className="tabular text-meta text-ink-3">{color.hex ?? "—"}</Td>
                  <Td numeric className="text-ink-2">
                    {color.productCount}
                  </Td>
                  <Td numeric className="text-ink-2">
                    {color.sortOrder}
                  </Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        )}
      </PageBody>
      {editing && (
        <ColorSheet key={editing === "new" ? "new" : editing.id} color={editing === "new" ? null : editing} onClose={() => setEditing(null)} />
      )}
    </>
  );
}

function ColorSheet({ color, onClose }: { color: AdminColor | null; onClose: () => void }) {
  const client = useQueryClient();
  const flash = useFlash();
  const [code, setCode] = useState(color?.code ?? "");
  const [hex, setHex] = useState(color?.hex ?? "");
  const [sortOrder, setSortOrder] = useState(String(color?.sortOrder ?? 0));
  const [names, setNames] = useState<Record<Language, string>>(
    () => Object.fromEntries(LANGUAGES.map((l) => [l, color?.names[l] ?? ""])) as Record<Language, string>,
  );
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [confirmDelete, setConfirmDelete] = useState(false);

  const save = useMutation({
    mutationFn: (body: Record<string, unknown>) =>
      color ? adminApi.put(`/colors/${color.id}`, body) : adminApi.post("/colors", body),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: dictionaryKeys.colors });
      flash(color ? "Zapisano kolor" : "Dodano kolor");
      onClose();
    },
    onError: (error) => {
      if (error instanceof ApiError && error.code === "data.concurrent-modification") {
        client.invalidateQueries({ queryKey: dictionaryKeys.colors });
      }
    },
  });

  const remove = useMutation({
    mutationFn: () => adminApi.delete(`/colors/${color!.id}`),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: dictionaryKeys.colors });
      flash("Usunięto kolor");
      setConfirmDelete(false);
      onClose();
    },
  });

  const submit = () => {
    const next: Record<string, string> = {};
    if (!code.trim()) next.code = "Kod jest wymagany, np. NAVY-02.";
    if (hex && !HEX.test(hex)) next.hex = "Format #RRGGBB, np. #1C2340.";
    if (!names.pl.trim()) next.pl = "Nazwa po polsku jest wymagana.";
    const order = parseOrder(sortOrder);
    if (order === null) next.sortOrder = "Podaj liczbę całkowitą ≥ 0.";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    save.mutate({
      code: code.trim(),
      hex: hex || null,
      sortOrder: order,
      names: Object.fromEntries(LANGUAGES.filter((l) => names[l].trim()).map((l) => [l, names[l].trim()])),
      ...(color ? { version: color.version } : {}),
    });
  };

  return (
    <>
      <FormSheet
        open
        onOpenChange={(open) => !open && onClose()}
        title={color ? `Kolor: ${colorName(color)}` : "Nowy kolor"}
        description="Kod, próbka koloru i nazwy w językach katalogu."
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
        footer={
          <SheetActions
            onCancel={onClose}
            submitLabel={color ? "Zapisz zmiany" : "Dodaj kolor"}
            pending={save.isPending}
            error={save.isError ? errorMessage(save.error) : null}
            start={
              color && (
                <Button type="button" variant="danger" onClick={() => setConfirmDelete(true)}>
                  Usuń
                </Button>
              )
            }
          />
        }
      >
        <div className="grid grid-cols-[1fr_120px] gap-4">
          <Field label="Kod" error={errors.code} hint="Wewnętrzny, np. NAVY-02 — trafia do SKU.">
            <TextInput value={code} maxLength={40} onChange={(e) => setCode(e.target.value.toUpperCase())} />
          </Field>
          <Field label="Kolejność" error={errors.sortOrder}>
            <NumberInput value={sortOrder} onChange={(e) => setSortOrder(e.target.value)} />
          </Field>
        </div>
        <Field label="Próbka (HEX)" error={errors.hex} hint="Kolor kwadracika przy modelu w katalogu.">
          <div className="flex items-center gap-2">
            <input
              type="color"
              aria-label="Wybierz kolor próbki"
              value={HEX.test(hex) ? hex : "#ffffff"}
              onChange={(e) => setHex(e.target.value.toUpperCase())}
              className="h-10 w-12 shrink-0 cursor-pointer border border-edge bg-background p-1"
            />
            <TextInput value={hex} maxLength={7} placeholder="#1C2340" onChange={(e) => setHex(e.target.value.toUpperCase())} className="tabular" />
          </div>
        </Field>
        {LANGUAGES.map((code) => (
          <Field
            key={code}
            label={code === "pl" ? "Nazwa — polski (wymagana)" : `Nazwa — ${LANGUAGE_LABEL[code].toLowerCase()}`}
            error={code === "pl" ? errors.pl : undefined}
            aside={code !== "pl" && !names[code].trim() && <span className="text-meta text-ink-3">brak</span>}
          >
            <TextInput lang={code} value={names[code]} maxLength={80} onChange={(e) => setNames((n) => ({ ...n, [code]: e.target.value }))} />
          </Field>
        ))}
      </FormSheet>
      {color && (
        <ConfirmDialog
          open={confirmDelete}
          onOpenChange={(open) => {
            setConfirmDelete(open);
            if (!open) remove.reset();
          }}
          title={`Usunąć kolor „${colorName(color)}”?`}
          confirmLabel="Usuń kolor"
          onConfirm={() => remove.mutate()}
          pending={remove.isPending}
        >
          {remove.isError ? (
            <span className="text-danger">{errorMessage(remove.error)}</span>
          ) : color.productCount > 0 ? (
            `Kolor jest używany w ${color.productCount} modelach — usunięcie się nie powiedzie.`
          ) : (
            "Tej operacji nie można cofnąć."
          )}
        </ConfirmDialog>
      )}
    </>
  );
}
