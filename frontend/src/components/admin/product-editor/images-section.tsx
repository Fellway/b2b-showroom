"use client";

import { plural } from "@/lib/admin/plural";
import Image from "next/image";
import { ArrowLeft, ArrowRight, Check, Trash2, Upload } from "lucide-react";
import { useId, useRef, useState, type DragEvent } from "react";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/admin/ui/field";
import { useFlash } from "@/components/admin/ui/flash";
import { adminApi } from "@/lib/admin/api";
import { errorMessage } from "@/lib/admin/errors";
import { useProductRefresh, type AdminProduct } from "@/lib/admin/product-editor";
import { cn } from "@/lib/utils";
import { Section } from "./section";

const ACCEPTED = ["image/jpeg", "image/png", "image/webp", "image/avif"];
const MAX_BYTES = 10 * 1024 * 1024;

type Upload = { key: string; name: string; state: "waiting" | "sending" | "done" | "error"; message?: string };

export function ImagesSection({ product }: { product: AdminProduct }) {
  const refresh = useProductRefresh(product.id);
  const flash = useFlash();
  const [pendingId, setPendingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const colorLabel = (colorId: string | null) => product.colors.find((c) => c.id === colorId)?.name ?? "Ogólne";

  async function run(imageId: string, action: () => Promise<unknown>, message: string) {
    setPendingId(imageId);
    setError(null);
    try {
      await action();
      await refresh();
      flash(message);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setPendingId(null);
    }
  }

  const move = (index: number, offset: number) => {
    const ids = product.images.map((i) => i.id);
    const [moved] = ids.splice(index, 1);
    ids.splice(index + offset, 0, moved);
    return run(moved, () => adminApi.put(`/products/${product.id}/images/order`, { imageIds: ids }), "Zmieniono kolejność zdjęć");
  };

  return (
    <Section
      id="zdjecia"
      title="Zdjęcia"
      description={
        product.images.length === 0
          ? "Brak zdjęć — w katalogu pojawi się pusty podkład. Pierwsze zdjęcie jest okładką modelu."
          : "Pierwsze zdjęcie jest okładką modelu. Zdjęcie przypisane do koloru pokazuje się po wybraniu tego koloru."
      }
    >
      {product.images.length > 0 && (
        <ol className="grid grid-cols-2 gap-x-4 gap-y-6 sm:grid-cols-3 xl:grid-cols-5">
          {product.images.map((image, index) => (
            <li key={image.id} className={cn("flex flex-col gap-2", pendingId === image.id && "opacity-60")}>
              <div className="relative aspect-[2/3] bg-well">
                <Image src={image.url} alt={`Zdjęcie ${index + 1}, ${colorLabel(image.colorId)}`} fill sizes="220px" className="object-cover" />
              </div>
              <p className="tabular text-meta text-ink-3">
                {index + 1}
                {index === 0 ? " · okładka" : ""}
              </p>
              <Select
                aria-label={`Kolor zdjęcia ${index + 1}`}
                value={image.colorId ?? ""}
                disabled={pendingId !== null}
                onChange={(e) =>
                  run(
                    image.id,
                    () => adminApi.put(`/products/${product.id}/images/${image.id}/color`, { colorId: e.target.value || null }),
                    "Przypisano kolor zdjęcia",
                  )
                }
              >
                <option value="">Ogólne</option>
                {product.colors.map((color) => (
                  <option key={color.id} value={color.id}>
                    {color.name}
                  </option>
                ))}
              </Select>
              <div className="flex items-center">
                <Button variant="icon" size="icon-sm" aria-label={`Przesuń zdjęcie ${index + 1} w lewo`} disabled={index === 0 || pendingId !== null} onClick={() => move(index, -1)}>
                  <ArrowLeft aria-hidden strokeWidth={1.5} />
                </Button>
                <Button
                  variant="icon"
                  size="icon-sm"
                  aria-label={`Przesuń zdjęcie ${index + 1} w prawo`}
                  disabled={index === product.images.length - 1 || pendingId !== null}
                  onClick={() => move(index, 1)}
                >
                  <ArrowRight aria-hidden strokeWidth={1.5} />
                </Button>
                <Button
                  variant="icon"
                  size="icon-sm"
                  className="ml-auto hover:text-danger"
                  aria-label={`Usuń zdjęcie ${index + 1}`}
                  disabled={pendingId !== null}
                  onClick={() => run(image.id, () => adminApi.delete(`/products/${product.id}/images/${image.id}`), "Usunięto zdjęcie")}
                >
                  <Trash2 aria-hidden strokeWidth={1.5} />
                </Button>
              </div>
            </li>
          ))}
        </ol>
      )}

      {error && (
        <p role="alert" className="mt-4 text-meta text-danger">
          {error}
        </p>
      )}

      <Uploader product={product} />
    </Section>
  );
}

function Uploader({ product }: { product: AdminProduct }) {
  const refresh = useProductRefresh(product.id);
  const flash = useFlash();
  const inputRef = useRef<HTMLInputElement>(null);
  const colorId = useId();
  const [target, setTarget] = useState("");
  const [uploads, setUploads] = useState<Upload[]>([]);
  const [dragging, setDragging] = useState(false);
  const busy = uploads.some((u) => u.state === "sending" || u.state === "waiting");

  const patch = (key: string, next: Partial<Upload>) => setUploads((list) => list.map((u) => (u.key === key ? { ...u, ...next } : u)));

  async function send(files: File[]) {
    if (files.length === 0) return;
    const batch = files.map((file, i) => ({ file, key: `${Date.now()}-${i}-${file.name}` }));
    setUploads(batch.map(({ file, key }) => ({ key, name: file.name, state: "waiting" })));
    let done = 0;
    // po kolei — każde zdjęcie zmienia kolejność i wersję modelu
    for (const { file, key } of batch) {
      if (!ACCEPTED.includes(file.type)) {
        patch(key, { state: "error", message: "Nieobsługiwany format. Wgraj JPEG, PNG, WebP lub AVIF." });
        continue;
      }
      if (file.size > MAX_BYTES) {
        patch(key, { state: "error", message: "Plik jest większy niż 10 MB." });
        continue;
      }
      patch(key, { state: "sending" });
      const form = new FormData();
      form.append("file", file);
      try {
        await adminApi.upload(`/products/${product.id}/images${target ? `?colorId=${target}` : ""}`, form);
        patch(key, { state: "done" });
        done++;
      } catch (e) {
        patch(key, { state: "error", message: errorMessage(e) });
      }
    }
    await refresh();
    if (done > 0) flash(done === 1 ? "Wgrano zdjęcie" : `Wgrano ${plural(done, "zdjęcie", "zdjęcia", "zdjęć")}`);
  }

  function onDrop(event: DragEvent) {
    event.preventDefault();
    setDragging(false);
    if (!busy) void send([...event.dataTransfer.files]);
  }

  return (
    <div className="mt-8">
      <div className="mb-3 flex flex-wrap items-end gap-3">
        <div className="flex w-64 flex-col gap-1.5">
          <label htmlFor={colorId} className="text-meta text-ink-3">
            Kolor nowych zdjęć
          </label>
          <Select id={colorId} value={target} onChange={(e) => setTarget(e.target.value)}>
            <option value="">Ogólne (bez koloru)</option>
            {product.colors.map((color) => (
              <option key={color.id} value={color.id}>
                {color.name}
              </option>
            ))}
          </Select>
        </div>
      </div>
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        className={cn(
          "flex flex-col items-center justify-center gap-3 border border-dashed px-6 py-10 text-center transition-colors",
          dragging ? "border-foreground bg-well" : "border-edge",
        )}
      >
        <Upload aria-hidden className="size-5 text-ink-3" strokeWidth={1.5} />
        <p className="text-ink-2">Przeciągnij zdjęcia tutaj albo</p>
        <Button variant="secondary" disabled={busy} onClick={() => inputRef.current?.click()}>
          Wybierz pliki
        </Button>
        <p className="text-meta text-ink-3">JPEG, PNG, WebP lub AVIF, do 10 MB. Najlepiej pionowe 2:3.</p>
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED.join(",")}
          multiple
          className="sr-only"
          tabIndex={-1}
          onChange={(e) => {
            void send([...(e.target.files ?? [])]);
            e.target.value = "";
          }}
        />
      </div>
      {uploads.length > 0 && (
        <ul aria-live="polite" className="mt-3 flex flex-col">
          {uploads.map((upload) => (
            <li key={upload.key} className="flex items-center gap-3 border-t border-line py-2 text-small">
              <span className="min-w-0 flex-1 truncate">{upload.name}</span>
              <span className={cn("shrink-0", upload.state === "error" ? "text-danger" : "text-ink-3")}>
                {upload.state === "waiting" && "w kolejce"}
                {upload.state === "sending" && "wysyłanie…"}
                {upload.state === "done" && (
                  <span className="inline-flex items-center gap-1 text-foreground">
                    <Check aria-hidden className="size-4" strokeWidth={1.5} />
                    gotowe
                  </span>
                )}
                {upload.state === "error" && upload.message}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
