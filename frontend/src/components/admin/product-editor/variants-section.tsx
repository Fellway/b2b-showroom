"use client";

import { plural } from "@/lib/admin/plural";
import { ArrowDown, ArrowUp, Plus, X } from "lucide-react";
import { useMemo, useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/admin/ui/confirm-dialog";
import { Select } from "@/components/admin/ui/field";
import { useFlash } from "@/components/admin/ui/flash";
import { Table, Td, Th, Tr } from "@/components/admin/ui/table";
import { adminApi } from "@/lib/admin/api";
import { colorName, useColors, useSizes, type AdminSize } from "@/lib/admin/dictionaries";
import { errorMessage } from "@/lib/admin/errors";
import { gtinProblem, suggestSku, useProductRefresh, type AdminProduct } from "@/lib/admin/product-editor";
import { cn } from "@/lib/utils";
import { Section } from "./section";

type Variant = AdminProduct["variants"][number];
type ProductColor = AdminProduct["colors"][number];

const cellInput =
  "tabular h-9 w-full border border-edge bg-background px-2.5 text-small outline-none transition-colors hover:border-foreground focus-visible:border-foreground aria-invalid:border-danger";

export function VariantsSection({ product }: { product: AdminProduct }) {
  const colors = useColors();
  const sizes = useSizes();
  const refresh = useProductRefresh(product.id);
  const flash = useFlash();
  const [newColor, setNewColor] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const sizeOrder = useMemo(() => new Map((sizes.data ?? []).map((s) => [s.id, s.sortOrder])), [sizes.data]);
  const available = (colors.data ?? []).filter((c) => !product.colors.some((pc) => pc.id === c.id));
  const activeCount = product.variants.filter((v) => v.active).length;

  async function addColor(event: FormEvent) {
    event.preventDefault();
    if (!newColor) return;
    setPending(true);
    setError(null);
    try {
      await adminApi.post(`/products/${product.id}/colors`, { colorId: newColor });
      await refresh();
      setNewColor("");
      flash("Dodano kolor");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setPending(false);
    }
  }

  return (
    <Section
      id="warianty"
      title="Kolory i rozmiary"
      description={
        product.variants.length === 0
          ? "Brak wariantów — model nie może zostać opublikowany bez przynajmniej jednego aktywnego rozmiaru."
          : `${plural(product.colors.length, "kolor", "kolory", "kolorów")} · ${activeCount} z ${product.variants.length} wariantów aktywnych.`
      }
    >
      <div className="flex flex-col gap-10">
        {product.colors.map((color, index) => (
          <ColorBlock
            key={color.id}
            product={product}
            color={color}
            index={index}
            sizes={sizes.data ?? []}
            sizeOrder={sizeOrder}
          />
        ))}
      </div>

      <form onSubmit={addColor} className="mt-10 flex flex-wrap items-end gap-3 border-t border-line pt-6">
        <div className="flex w-72 flex-col gap-1.5">
          <label htmlFor="add-color" className="text-meta text-ink-3">
            Dodaj kolor
          </label>
          <Select id="add-color" value={newColor} onChange={(e) => setNewColor(e.target.value)} disabled={available.length === 0}>
            <option value="">{available.length === 0 ? "Wszystkie kolory są już dodane" : "Wybierz kolor…"}</option>
            {available.map((c) => (
              <option key={c.id} value={c.id}>
                {colorName(c)} ({c.code})
              </option>
            ))}
          </Select>
        </div>
        <Button type="submit" variant="secondary" disabled={!newColor || pending}>
          <Plus aria-hidden strokeWidth={1.5} />
          {pending ? "Dodawanie…" : "Dodaj kolor"}
        </Button>
        {error && (
          <p role="alert" className="basis-full text-meta text-danger">
            {error}
          </p>
        )}
      </form>
    </Section>
  );
}

function ColorBlock({
  product,
  color,
  index,
  sizes,
  sizeOrder,
}: {
  product: AdminProduct;
  color: ProductColor;
  index: number;
  sizes: AdminSize[];
  sizeOrder: Map<string, number>;
}) {
  const refresh = useProductRefresh(product.id);
  const flash = useFlash();
  const [confirmRemove, setConfirmRemove] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);

  const variants = product.variants
    .filter((v) => v.colorId === color.id)
    .sort((a, b) => (sizeOrder.get(a.sizeId) ?? 0) - (sizeOrder.get(b.sizeId) ?? 0));
  const imageCount = product.images.filter((i) => i.colorId === color.id).length;
  const last = index === product.colors.length - 1;

  async function run(action: () => Promise<unknown>, message: string) {
    setPending(true);
    setError(null);
    try {
      await action();
      await refresh();
      flash(message);
      return true;
    } catch (e) {
      setError(errorMessage(e));
      return false;
    } finally {
      setPending(false);
    }
  }

  const move = (offset: number) => {
    const ids = product.colors.map((c) => c.id);
    const [moved] = ids.splice(index, 1);
    ids.splice(index + offset, 0, moved);
    return run(() => adminApi.put(`/products/${product.id}/colors/order`, { colorIds: ids }), "Zmieniono kolejność kolorów");
  };

  return (
    <div aria-labelledby={`color-${color.id}`}>
      <div className="flex flex-wrap items-center gap-3 pb-3">
        <span aria-hidden className="size-4 border border-black/15" style={{ backgroundColor: color.hex ?? "transparent" }} />
        <h3 id={`color-${color.id}`} className="font-medium">
          {color.name}
        </h3>
        <span className="tabular text-meta text-ink-3">{color.code}</span>
        <div className="ml-auto flex items-center">
          <Button variant="icon" size="icon-sm" aria-label={`Przesuń ${color.name} wyżej`} disabled={index === 0 || pending} onClick={() => move(-1)}>
            <ArrowUp aria-hidden strokeWidth={1.5} />
          </Button>
          <Button variant="icon" size="icon-sm" aria-label={`Przesuń ${color.name} niżej`} disabled={last || pending} onClick={() => move(1)}>
            <ArrowDown aria-hidden strokeWidth={1.5} />
          </Button>
          <Button variant="danger" size="sm" onClick={() => setConfirmRemove(true)} disabled={pending}>
            Usuń kolor
          </Button>
        </div>
      </div>

      {variants.length > 0 ? (
        <Table>
          <thead>
            <tr>
              <Th className="w-20">Rozmiar</Th>
              <Th>SKU</Th>
              <Th>EAN</Th>
              <Th className="w-24">Aktywny</Th>
              <Th className="w-28">
                <span className="sr-only">Akcje</span>
              </Th>
            </tr>
          </thead>
          <tbody>
            {variants.map((variant) => (
              <VariantRow key={`${variant.id}:${variant.sku}:${variant.ean}:${variant.active}`} product={product} variant={variant} />
            ))}
          </tbody>
        </Table>
      ) : (
        <p className="border-t border-line py-4 text-ink-3">Ten kolor nie ma jeszcze rozmiarów.</p>
      )}

      {error && (
        <p role="alert" className="mt-2 text-meta text-danger">
          {error}
        </p>
      )}

      {adding ? (
        <AddSizes
          product={product}
          color={color}
          sizes={sizes.filter((s) => !variants.some((v) => v.sizeId === s.id))}
          onDone={() => setAdding(false)}
        />
      ) : (
        <Button variant="text" size="sm" className="mt-2 -ml-3" onClick={() => setAdding(true)}>
          <Plus aria-hidden strokeWidth={1.5} />
          Dodaj rozmiary
        </Button>
      )}

      <ConfirmDialog
        open={confirmRemove}
        onOpenChange={setConfirmRemove}
        title={`Usunąć kolor „${color.name}”?`}
        confirmLabel="Usuń kolor"
        pending={pending}
        onConfirm={async () => {
          if (await run(() => adminApi.delete(`/products/${product.id}/colors/${color.id}`), "Usunięto kolor")) setConfirmRemove(false);
          else setConfirmRemove(false);
        }}
      >
        Usuniesz {plural(variants.length, "wariant", "warianty", "wariantów")}
        {imageCount > 0 ? ` i ${plural(imageCount, "zdjęcie", "zdjęcia", "zdjęć")}` : ""} tego koloru. Wysłane już zestawienia
        zachowują swoje dane.
      </ConfirmDialog>
    </div>
  );
}

function VariantRow({ product, variant }: { product: AdminProduct; variant: Variant }) {
  const refresh = useProductRefresh(product.id);
  const flash = useFlash();
  const [sku, setSku] = useState(variant.sku);
  const [ean, setEan] = useState(variant.ean);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const changed = sku.trim().toUpperCase() !== variant.sku || ean.trim() !== variant.ean;
  const eanError = changed ? gtinProblem(ean) : null;

  async function save(active = variant.active, message = "Zapisano wariant") {
    setPending(true);
    setError(null);
    try {
      await adminApi.put(`/products/${product.id}/variants/${variant.id}`, { sku: sku.trim(), ean: ean.trim(), active });
      await refresh();
      flash(message);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setPending(false);
    }
  }

  async function remove() {
    setPending(true);
    setError(null);
    try {
      await adminApi.delete(`/products/${product.id}/variants/${variant.id}`);
      await refresh();
      flash(`Usunięto rozmiar ${variant.size}`);
    } catch (e) {
      setError(errorMessage(e));
      setPending(false);
    }
  }

  return (
    <Tr className={cn(!variant.active && "text-ink-3")}>
      <Td className="tabular font-medium">{variant.size}</Td>
      <Td>
        <input aria-label={`SKU rozmiaru ${variant.size}`} className={cellInput} value={sku} maxLength={64} onChange={(e) => setSku(e.target.value)} />
      </Td>
      <Td>
        <input
          aria-label={`EAN rozmiaru ${variant.size}`}
          className={cellInput}
          value={ean}
          inputMode="numeric"
          maxLength={14}
          aria-invalid={Boolean(eanError) || undefined}
          onChange={(e) => setEan(e.target.value.replace(/\D/g, ""))}
        />
        {(eanError || error) && (
          <p role="alert" className="mt-1 text-meta text-danger">
            {eanError ?? error}
          </p>
        )}
      </Td>
      <Td>
        <label className="inline-flex cursor-pointer items-center gap-2">
          <input
            type="checkbox"
            className="size-4 accent-foreground"
            checked={variant.active}
            disabled={pending || changed}
            onChange={(e) => save(e.target.checked, e.target.checked ? `Rozmiar ${variant.size} aktywny` : `Rozmiar ${variant.size} wyłączony`)}
          />
          <span className="sr-only">Aktywny rozmiar {variant.size}</span>
          <span aria-hidden>{variant.active ? "Tak" : "Nie"}</span>
        </label>
      </Td>
      <Td className="text-right">
        {changed ? (
          <span className="inline-flex items-center gap-1">
            <Button variant="text" size="sm" onClick={() => { setSku(variant.sku); setEan(variant.ean); }} disabled={pending}>
              Cofnij
            </Button>
            <Button variant="secondary" size="sm" onClick={() => save()} disabled={pending || Boolean(eanError) || !sku.trim()}>
              Zapisz
            </Button>
          </span>
        ) : (
          <Button variant="icon" size="icon-sm" aria-label={`Usuń rozmiar ${variant.size}`} onClick={remove} disabled={pending}>
            <X aria-hidden strokeWidth={1.5} />
          </Button>
        )}
      </Td>
    </Tr>
  );
}

type NewRow = { sizeId: string; size: string; sku: string; ean: string; error?: string };

function AddSizes({
  product,
  color,
  sizes,
  onDone,
}: {
  product: AdminProduct;
  color: ProductColor;
  sizes: AdminSize[];
  onDone: () => void;
}) {
  const refresh = useProductRefresh(product.id);
  const flash = useFlash();
  const [rows, setRows] = useState<NewRow[]>([]);
  const [pending, setPending] = useState(false);
  const ordered = [...sizes].sort((a, b) => a.sortOrder - b.sortOrder);

  const toggle = (size: AdminSize) =>
    setRows((current) =>
      current.some((r) => r.sizeId === size.id)
        ? current.filter((r) => r.sizeId !== size.id)
        : [...current, { sizeId: size.id, size: size.code, sku: suggestSku(product.styleCode, color.code, size.code), ean: "" }].sort(
            (a, b) => ordered.findIndex((s) => s.id === a.sizeId) - ordered.findIndex((s) => s.id === b.sizeId),
          ),
    );

  const update = (sizeId: string, patch: Partial<NewRow>) =>
    setRows((current) => current.map((r) => (r.sizeId === sizeId ? { ...r, ...patch, error: undefined } : r)));

  const invalid = rows.some((r) => !r.sku.trim() || gtinProblem(r.ean));

  async function submit(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    const failed: NewRow[] = [];
    let added = 0;
    // po kolei: każdy wariant to osobna operacja na agregacie (wersja modelu rośnie przy każdej)
    for (const row of rows) {
      try {
        await adminApi.post(`/products/${product.id}/variants`, { colorId: color.id, sizeId: row.sizeId, sku: row.sku.trim(), ean: row.ean.trim() });
        added++;
      } catch (e) {
        failed.push({ ...row, error: errorMessage(e) });
      }
    }
    await refresh();
    setPending(false);
    if (added > 0) flash(`Dodano ${added} ${added === 1 ? "rozmiar" : added < 5 ? "rozmiary" : "rozmiarów"}`);
    if (failed.length === 0) onDone();
    else setRows(failed);
  }

  return (
    <form onSubmit={submit} className="mt-4 bg-well p-5">
      <div className="flex items-baseline justify-between">
        <p className="text-meta text-ink-3">Wybierz rozmiary, a potem wpisz lub wklej kody EAN</p>
        <Button variant="text" size="sm" onClick={onDone}>
          Anuluj
        </Button>
      </div>
      {ordered.length === 0 ? (
        <p className="mt-3 text-ink-2">Ten kolor ma już wszystkie rozmiary ze słownika.</p>
      ) : (
        <div className="mt-3 flex flex-wrap gap-1.5">
          {ordered.map((size) => {
            const active = rows.some((r) => r.sizeId === size.id);
            return (
              <button
                key={size.id}
                type="button"
                aria-pressed={active}
                onClick={() => toggle(size)}
                className={cn(
                  "tabular flex h-10 min-w-12 items-center justify-center border px-3 text-small transition-colors",
                  active ? "border-foreground bg-foreground text-background" : "border-edge bg-background hover:border-foreground",
                )}
              >
                {size.code}
              </button>
            );
          })}
        </div>
      )}

      {rows.length > 0 && (
        <div className="mt-5">
          <Table>
            <thead>
              <tr>
                <Th className="w-20">Rozmiar</Th>
                <Th>SKU</Th>
                <Th>EAN</Th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => {
                const eanError = row.ean ? gtinProblem(row.ean) : null;
                return (
                  <Tr key={row.sizeId}>
                    <Td className="tabular font-medium">{row.size}</Td>
                    <Td>
                      <input aria-label={`SKU rozmiaru ${row.size}`} className={cellInput} value={row.sku} maxLength={64} onChange={(e) => update(row.sizeId, { sku: e.target.value })} />
                    </Td>
                    <Td>
                      <input
                        aria-label={`EAN rozmiaru ${row.size}`}
                        className={cellInput}
                        value={row.ean}
                        inputMode="numeric"
                        maxLength={14}
                        placeholder="13 cyfr"
                        aria-invalid={Boolean(eanError) || undefined}
                        onChange={(e) => update(row.sizeId, { ean: e.target.value.replace(/\D/g, "") })}
                      />
                      {(eanError || row.error) && (
                        <p role="alert" className="mt-1 text-meta text-danger">
                          {row.error ?? eanError}
                        </p>
                      )}
                    </Td>
                  </Tr>
                );
              })}
            </tbody>
          </Table>
          <Button type="submit" variant="secondary" className="mt-4" disabled={pending || invalid}>
            {pending ? "Dodawanie…" : `Dodaj ${rows.length} ${rows.length === 1 ? "rozmiar" : rows.length < 5 ? "rozmiary" : "rozmiarów"}`}
          </Button>
        </div>
      )}
    </form>
  );
}
