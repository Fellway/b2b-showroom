"use client";

import { ActiveLabel } from "@/components/admin/ui/status-label";
import { plural } from "@/lib/admin/plural";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/admin/ui/confirm-dialog";
import { Checkbox, Field, NumberInput, Select, TextArea, TextInput } from "@/components/admin/ui/field";
import { useFlash } from "@/components/admin/ui/flash";
import { PageBody, PageHeader } from "@/components/admin/ui/page-header";
import { EmptyState, ErrorState, LoadingRows } from "@/components/admin/ui/states";
import { Table, Td, Th, Tr } from "@/components/admin/ui/table";
import { adminApi, ApiError } from "@/lib/admin/api";
import {
  categoryName,
  dictionaryKeys,
  LANGUAGE_LABEL,
  LANGUAGES,
  useCategories,
  type AdminCategory,
  type Language,
} from "@/lib/admin/dictionaries";
import { errorMessage } from "@/lib/admin/errors";
import { FieldGroup, FormSheet, SheetActions } from "@/components/admin/ui/form-sheet";
import { missingLanguages, missingText, parseOrder, slugify } from "./helpers";

type Row = { category: AdminCategory; depth: number };

/** Drzewo kategorii spłaszczone do wierszy: rodzic, potem jego dzieci (wg kolejności), wcięte o poziom. */
function flatten(categories: AdminCategory[]): Row[] {
  const byParent = new Map<string | null, AdminCategory[]>();
  for (const c of categories) {
    const key = c.parentId && categories.some((p) => p.id === c.parentId) ? c.parentId : null;
    byParent.set(key, [...(byParent.get(key) ?? []), c]);
  }
  const rows: Row[] = [];
  const walk = (parent: string | null, depth: number) => {
    const children = (byParent.get(parent) ?? []).sort(
      (a, b) => a.sortOrder - b.sortOrder || categoryName(a).localeCompare(categoryName(b), "pl"),
    );
    for (const child of children) {
      rows.push({ category: child, depth });
      walk(child.id, depth + 1);
    }
  };
  walk(null, 0);
  return rows;
}

function descendantsOf(id: string, categories: AdminCategory[]): Set<string> {
  const result = new Set<string>([id]);
  let grew = true;
  while (grew) {
    grew = false;
    for (const c of categories) {
      if (c.parentId && result.has(c.parentId) && !result.has(c.id)) {
        result.add(c.id);
        grew = true;
      }
    }
  }
  return result;
}

export function CategoriesView() {
  const categories = useCategories();
  const [editing, setEditing] = useState<AdminCategory | "new" | null>(null);
  const rows = useMemo(() => flatten(categories.data ?? []), [categories.data]);

  return (
    <>
      <PageHeader
        title="Kategorie"
        meta={categories.data ? <span className="tabular">{categories.data.length}</span> : undefined}
        actions={
          <Button onClick={() => setEditing("new")}>
            <Plus aria-hidden strokeWidth={1.5} />
            Nowa kategoria
          </Button>
        }
      />
      <PageBody>
        {categories.isPending ? (
          <LoadingRows rows={4} />
        ) : categories.isError ? (
          <ErrorState message={errorMessage(categories.error)} onRetry={() => categories.refetch()} />
        ) : rows.length === 0 ? (
          <EmptyState title="Nie ma jeszcze kategorii.">Każdy model należy do kategorii — dodaj pierwszą, np. „Sukienki”.</EmptyState>
        ) : (
          <Table>
            <thead>
              <tr>
                <Th>Nazwa</Th>
                <Th>Adres</Th>
                <Th>Tłumaczenia</Th>
                <Th>Widoczność</Th>
                <Th numeric>Modele</Th>
                <Th numeric>Kolejność</Th>
              </tr>
            </thead>
            <tbody>
              {rows.map(({ category, depth }) => (
                <Tr key={category.id} interactive onClick={() => setEditing(category)}>
                  <Td>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setEditing(category);
                      }}
                      style={{ paddingLeft: `${depth * 24}px` }}
                      className="inline-flex items-center gap-2 text-left font-medium underline-offset-4 hover:underline"
                    >
                      {depth > 0 && <span aria-hidden className="text-ink-3">└</span>}
                      {categoryName(category)}
                    </button>
                  </Td>
                  <Td className="text-ink-2">{category.slug}</Td>
                  <Td className="text-meta text-ink-3">{missingText(missingLanguages(category.translations))}</Td>
                  <Td>
                    {category.visible ? (
                      <ActiveLabel active activeText="Widoczna" />
                    ) : (
                      <ActiveLabel active={false} inactiveText="Ukryta" />
                    )}
                  </Td>
                  <Td numeric className="text-ink-2">
                    {category.productCount}
                  </Td>
                  <Td numeric className="text-ink-2">
                    {category.sortOrder}
                  </Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        )}
      </PageBody>
      {editing && (
        <CategorySheet
          key={editing === "new" ? "new" : editing.id}
          category={editing === "new" ? null : editing}
          all={categories.data ?? []}
          onClose={() => setEditing(null)}
        />
      )}
    </>
  );
}

type Translation = { name: string; description: string };

function CategorySheet({ category, all, onClose }: { category: AdminCategory | null; all: AdminCategory[]; onClose: () => void }) {
  const client = useQueryClient();
  const flash = useFlash();
  const [translations, setTranslations] = useState<Record<Language, Translation>>(() =>
    Object.fromEntries(
      LANGUAGES.map((code) => [
        code,
        { name: category?.translations[code]?.name ?? "", description: category?.translations[code]?.description ?? "" },
      ]),
    ) as Record<Language, Translation>,
  );
  const [slug, setSlug] = useState(category?.slug ?? "");
  const [slugTouched, setSlugTouched] = useState(Boolean(category));
  const [parentId, setParentId] = useState(category?.parentId ?? "");
  const [visible, setVisible] = useState(category?.visible ?? true);
  const [sortOrder, setSortOrder] = useState(String(category?.sortOrder ?? 0));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [confirmDelete, setConfirmDelete] = useState(false);

  const excluded = useMemo(() => (category ? descendantsOf(category.id, all) : new Set<string>()), [category, all]);
  const parents = flatten(all).filter(({ category: c }) => !excluded.has(c.id));

  const save = useMutation({
    mutationFn: (body: Record<string, unknown>) =>
      category ? adminApi.put(`/categories/${category.id}`, body) : adminApi.post("/categories", body),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: dictionaryKeys.categories });
      flash(category ? "Zapisano kategorię" : "Dodano kategorię");
      onClose();
    },
    onError: (error) => {
      if (error instanceof ApiError && error.code === "data.concurrent-modification") {
        client.invalidateQueries({ queryKey: dictionaryKeys.categories });
      }
    },
  });

  const remove = useMutation({
    mutationFn: () => adminApi.delete(`/categories/${category!.id}`),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: dictionaryKeys.categories });
      flash("Usunięto kategorię");
      setConfirmDelete(false);
      onClose();
    },
  });

  const setTranslation = (code: Language, patch: Partial<Translation>) => {
    setTranslations((current) => ({ ...current, [code]: { ...current[code], ...patch } }));
    if (code === "pl" && patch.name !== undefined && !slugTouched) setSlug(slugify(patch.name));
  };

  const submit = () => {
    const next: Record<string, string> = {};
    if (!translations.pl.name.trim()) next.name = "Nazwa po polsku jest wymagana.";
    if (!/^[a-z0-9]+(-[a-z0-9]+)*$/.test(slug)) next.slug = "Adres: małe litery, cyfry i pojedyncze myślniki.";
    const order = parseOrder(sortOrder);
    if (order === null) next.sortOrder = "Podaj liczbę całkowitą ≥ 0.";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    const payload = Object.fromEntries(
      LANGUAGES.filter((code) => translations[code].name.trim()).map((code) => [
        code,
        { name: translations[code].name.trim(), description: translations[code].description.trim() || null },
      ]),
    );
    save.mutate({
      parentId: parentId || null,
      slug,
      sortOrder: order,
      visible,
      translations: payload,
      ...(category ? { version: category.version } : {}),
    });
  };

  return (
    <>
      <FormSheet
        open
        onOpenChange={(open) => !open && onClose()}
        title={category ? `Kategoria: ${categoryName(category)}` : "Nowa kategoria"}
        description="Nazwa i opis kategorii w językach katalogu, adres, kategoria nadrzędna i widoczność."
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
        footer={
          <SheetActions
            onCancel={onClose}
            submitLabel={category ? "Zapisz zmiany" : "Dodaj kategorię"}
            pending={save.isPending}
            error={save.isError ? errorMessage(save.error) : null}
            start={
              category && (
                <Button type="button" variant="danger" onClick={() => setConfirmDelete(true)}>
                  Usuń
                </Button>
              )
            }
          />
        }
      >
        <Field label="Nazwa (polski — wymagana)" error={errors.name}>
          <TextInput value={translations.pl.name} maxLength={120} onChange={(e) => setTranslation("pl", { name: e.target.value })} />
        </Field>
        <Field label="Opis (polski)" hint="Wyświetlany pod tytułem kategorii w katalogu.">
          <TextArea rows={3} maxLength={5000} value={translations.pl.description} onChange={(e) => setTranslation("pl", { description: e.target.value })} />
        </Field>
        <div className="grid grid-cols-[1fr_120px] gap-4">
          <Field label="Adres (slug)" error={errors.slug} hint="Z nazwy polskiej; zmień, jeśli trzeba.">
            <TextInput
              value={slug}
              maxLength={120}
              onChange={(e) => {
                setSlugTouched(true);
                setSlug(e.target.value.toLowerCase());
              }}
            />
          </Field>
          <Field label="Kolejność" error={errors.sortOrder}>
            <NumberInput value={sortOrder} onChange={(e) => setSortOrder(e.target.value)} />
          </Field>
        </div>
        <Field label="Kategoria nadrzędna">
          <Select value={parentId} onChange={(e) => setParentId(e.target.value)}>
            <option value="">— brak (kategoria główna)</option>
            {parents.map(({ category: c, depth }) => (
              <option key={c.id} value={c.id}>
                {"  ".repeat(depth * 2)}
                {categoryName(c)}
              </option>
            ))}
          </Select>
        </Field>
        <Checkbox label="Widoczna w katalogu" checked={visible} onChange={(e) => setVisible(e.target.checked)} />

        {LANGUAGES.filter((code) => code !== "pl").map((code) => (
          <FieldGroup
            key={code}
            title={LANGUAGE_LABEL[code]}
            aside={!translations[code].name.trim() && <span className="text-meta text-ink-3">brak — katalog pokaże polski</span>}
          >
            <Field label="Nazwa">
              <TextInput lang={code} value={translations[code].name} maxLength={120} onChange={(e) => setTranslation(code, { name: e.target.value })} />
            </Field>
            <Field label="Opis">
              <TextArea lang={code} rows={2} maxLength={5000} value={translations[code].description} onChange={(e) => setTranslation(code, { description: e.target.value })} />
            </Field>
          </FieldGroup>
        ))}
      </FormSheet>
      {category && (
        <ConfirmDialog
          open={confirmDelete}
          onOpenChange={(open) => {
            setConfirmDelete(open);
            if (!open) remove.reset();
          }}
          title={`Usunąć kategorię „${categoryName(category)}”?`}
          confirmLabel="Usuń kategorię"
          onConfirm={() => remove.mutate()}
          pending={remove.isPending}
        >
          {remove.isError ? (
            <span className="text-danger">{errorMessage(remove.error)}</span>
          ) : category.productCount > 0 ? (
            `Kategoria ma ${plural(category.productCount, "model", "modele", "modeli")} — usunięcie się nie powiedzie, dopóki ich nie przeniesiesz.`
          ) : (
            "Tej operacji nie można cofnąć."
          )}
        </ConfirmDialog>
      )}
    </>
  );
}
