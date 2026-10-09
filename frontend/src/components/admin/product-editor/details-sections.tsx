"use client";

import { useState } from "react";
import { Checkbox, Field, NumberInput, Select, TextArea, TextInput } from "@/components/admin/ui/field";
import { LANGUAGE_LABEL, useCategories, type AdminCategory, type Language } from "@/lib/admin/dictionaries";
import {
  isTranslationFilled,
  parseMoney,
  slugify,
  type DraftErrors,
  type ProductDraft,
  type TranslationDraft,
} from "@/lib/admin/product-editor";
import { cn } from "@/lib/utils";
import { Section } from "./section";

type Props = {
  draft: ProductDraft;
  errors: DraftErrors;
  onChange: (update: (draft: ProductDraft) => ProductDraft) => void;
};

const TARGETS: Exclude<Language, "pl">[] = ["en", "cs", "uk"];

export function DetailsSection({ draft, errors, onChange }: Props) {
  const categories = useCategories();
  const pl = draft.translations.pl;
  const wholesale = parseMoney(draft.wholesale);
  const retail = parseMoney(draft.retail);
  const markup = wholesale && retail ? retail / wholesale : null;

  const setPl = (field: keyof TranslationDraft, value: string) =>
    onChange((d) => {
      const translations = { ...d.translations, pl: { ...d.translations.pl, [field]: value } };
      // adres podąża za nazwą, dopóki administrator nie zmieni go ręcznie
      return field === "name" && !d.slugTouched ? { ...d, translations, slug: slugify(value) } : { ...d, translations };
    });

  return (
    <Section id="dane" title="Dane" description="Nazwa i opis po polsku są wzorcem dla tłumaczeń i językiem zapasowym katalogu.">
      <div className="grid gap-x-6 gap-y-5 md:grid-cols-2">
        <Field label="Nazwa (polski)" error={errors.name} className="md:col-span-2">
          <TextInput value={pl.name} maxLength={200} onChange={(e) => setPl("name", e.target.value)} placeholder="np. Sukienka midi Klara" />
        </Field>
        <Field label="Kategoria" error={errors.categoryId}>
          <Select value={draft.categoryId} onChange={(e) => onChange((d) => ({ ...d, categoryId: e.target.value }))}>
            <option value="">Wybierz…</option>
            {orderedCategories(categories.data ?? []).map(({ category, depth }) => (
              <option key={category.id} value={category.id}>
                {"— ".repeat(depth)}
                {category.translations.pl?.name ?? category.slug}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Kod modelu" error={errors.styleCode} hint="Numer modelu z produkcji, np. FS-2601.">
          <TextInput
            value={draft.styleCode}
            maxLength={40}
            className="tabular uppercase"
            onChange={(e) => onChange((d) => ({ ...d, styleCode: e.target.value.toUpperCase() }))}
          />
        </Field>
        <Field
          label="Adres w katalogu"
          error={errors.slug}
          hint={`/p/${draft.slug || "…"}`}
          className="md:col-span-2"
          aside={
            draft.slugTouched && pl.name ? (
              <button
                type="button"
                className="text-meta underline underline-offset-4 hover:no-underline"
                onClick={() => onChange((d) => ({ ...d, slug: slugify(d.translations.pl.name), slugTouched: false }))}
              >
                Utwórz z nazwy
              </button>
            ) : undefined
          }
        >
          <TextInput
            value={draft.slug}
            maxLength={160}
            onChange={(e) => onChange((d) => ({ ...d, slug: e.target.value.toLowerCase(), slugTouched: true }))}
          />
        </Field>
      </div>

      <div className="mt-8 grid gap-x-6 gap-y-5 border-t border-line pt-6 md:grid-cols-3">
        <Field label="Cena hurtowa netto (PLN)" error={errors.wholesale}>
          <NumberInput value={draft.wholesale} placeholder="0,00" onChange={(e) => onChange((d) => ({ ...d, wholesale: e.target.value }))} />
        </Field>
        <Field label="Sugerowana cena detaliczna brutto (PLN)" error={errors.retail}>
          <NumberInput value={draft.retail} placeholder="0,00" onChange={(e) => onChange((d) => ({ ...d, retail: e.target.value }))} />
        </Field>
        <div className="flex flex-col gap-1.5">
          <span className="text-meta text-ink-3">Mnożnik detaliczny</span>
          <p className="tabular flex h-10 items-center text-body" aria-live="polite">
            {markup ? `×${markup.toFixed(2).replace(".", ",")}` : "—"}
          </p>
        </div>
      </div>

      <div className="mt-8 grid gap-x-6 gap-y-5 border-t border-line pt-6 md:grid-cols-2">
        <Field label="Opis (polski)" className="md:col-span-2">
          <TextArea value={pl.description} maxLength={10000} rows={4} onChange={(e) => setPl("description", e.target.value)} />
        </Field>
        <Field label="Skład (polski)" hint="Zostaw puste, jeśli nie znasz dokładnego składu.">
          <TextInput value={pl.materialComposition} maxLength={500} onChange={(e) => setPl("materialComposition", e.target.value)} />
        </Field>
        <Field label="Pielęgnacja (polski)">
          <TextInput value={pl.careInstructions} maxLength={2000} onChange={(e) => setPl("careInstructions", e.target.value)} />
        </Field>
      </div>

      <div className="mt-8 grid gap-x-4 gap-y-5 border-t border-line pt-6 sm:grid-cols-2">
        <Field label="Kolejność" error={errors.sortOrder} hint="Niższa liczba = wyżej w katalogu.">
          <NumberInput
            value={draft.sortOrder}
            className="w-28"
            inputMode="numeric"
            onChange={(e) => onChange((d) => ({ ...d, sortOrder: e.target.value }))}
          />
        </Field>
        <div className="flex flex-col gap-1.5">
          <span className="text-meta text-ink-3">Wyróżnienie</span>
          <div className="flex h-10 items-center">
            <Checkbox
              label="Wyróżniony — pokazuj na początku katalogu"
              checked={draft.featured}
              onChange={(e) => onChange((d) => ({ ...d, featured: e.target.checked }))}
            />
          </div>
        </div>
      </div>
    </Section>
  );
}

export function TranslationsSection({ draft, errors, onChange }: Props) {
  const [target, setTarget] = useState<Exclude<Language, "pl">>(
    () => TARGETS.find((language) => !isTranslationFilled(draft.translations[language])) ?? "en",
  );
  const pl = draft.translations.pl;
  const current = draft.translations[target];
  const missing = TARGETS.filter((language) => !isTranslationFilled(draft.translations[language]));

  const set = (field: keyof TranslationDraft, value: string) =>
    onChange((d) => ({ ...d, translations: { ...d.translations, [target]: { ...d.translations[target], [field]: value } } }));

  const rows: { field: keyof TranslationDraft; label: string; multiline?: boolean; max: number }[] = [
    { field: "name", label: "Nazwa", max: 200 },
    { field: "description", label: "Opis", multiline: true, max: 10000 },
    { field: "materialComposition", label: "Skład", max: 500 },
    { field: "careInstructions", label: "Pielęgnacja", max: 2000 },
  ];

  return (
    <Section
      id="tlumaczenia"
      title="Tłumaczenia"
      description={
        missing.length === 0
          ? "Wszystkie języki uzupełnione."
          : `Brak: ${missing.map((l) => LANGUAGE_LABEL[l].toLowerCase()).join(", ")}. Bez tłumaczenia katalog pokazuje tekst polski.`
      }
    >
      <div role="tablist" aria-label="Język tłumaczenia" className="flex gap-1 border-b border-line">
        {TARGETS.map((language) => {
          const filled = isTranslationFilled(draft.translations[language]);
          const active = language === target;
          return (
            <button
              key={language}
              type="button"
              role="tab"
              aria-selected={active}
              onClick={() => setTarget(language)}
              className={cn(
                "relative -mb-px flex h-10 items-center gap-2 border-b px-3 transition-colors",
                active ? "border-foreground text-foreground" : "border-transparent text-ink-2 hover:text-foreground",
              )}
            >
              <span
                aria-hidden
                className={cn("size-2 rounded-full border border-foreground", filled ? "bg-foreground" : "bg-transparent")}
              />
              {LANGUAGE_LABEL[language]}
              <span className="sr-only">{filled ? "(uzupełniony)" : "(brak)"}</span>
            </button>
          );
        })}
      </div>

      {errors.translations?.[target] && (
        <p role="alert" className="mt-4 text-meta text-danger">
          {errors.translations[target]}
        </p>
      )}

      <div role="tabpanel" className="mt-2">
        <div className="hidden grid-cols-2 gap-6 pt-4 text-meta text-ink-3 md:grid">
          <span>Polski — wzorzec</span>
          <span lang={target}>{LANGUAGE_LABEL[target]} — wklej tłumaczenie</span>
        </div>
        {rows.map((row) => (
          <div key={row.field} className="grid gap-x-6 gap-y-2 border-b border-line py-4 md:grid-cols-2">
            <div className="flex flex-col gap-1.5">
              <span className="text-meta text-ink-3">{row.label}</span>
              <p className={cn("whitespace-pre-line text-small", pl[row.field] ? "text-ink-2" : "text-ink-3")}>
                {pl[row.field] || "— brak w języku polskim —"}
              </p>
            </div>
            <Field label={<span className="md:sr-only">{`${row.label} (${LANGUAGE_LABEL[target].toLowerCase()})`}</span>}>
              {row.multiline ? (
                <TextArea lang={target} rows={4} maxLength={row.max} value={current[row.field]} onChange={(e) => set(row.field, e.target.value)} />
              ) : (
                <TextInput lang={target} maxLength={row.max} value={current[row.field]} onChange={(e) => set(row.field, e.target.value)} />
              )}
            </Field>
          </div>
        ))}
      </div>
    </Section>
  );
}

function orderedCategories(categories: AdminCategory[]) {
  const children = new Map<string | null, AdminCategory[]>();
  for (const category of categories) {
    const list = children.get(category.parentId) ?? [];
    list.push(category);
    children.set(category.parentId, list);
  }
  const result: { category: AdminCategory; depth: number }[] = [];
  const walk = (parent: string | null, depth: number) => {
    for (const category of (children.get(parent) ?? []).sort((a, b) => a.sortOrder - b.sortOrder)) {
      result.push({ category, depth });
      walk(category.id, depth + 1);
    }
  };
  walk(null, 0);
  return result;
}
