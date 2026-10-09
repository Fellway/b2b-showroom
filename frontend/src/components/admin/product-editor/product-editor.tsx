"use client";

import { plural } from "@/lib/admin/plural";
import { useQueryClient } from "@tanstack/react-query";
import { ExternalLink } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/admin/ui/confirm-dialog";
import { useFlash } from "@/components/admin/ui/flash";
import { PageHeader } from "@/components/admin/ui/page-header";
import { ErrorState, LoadingRows } from "@/components/admin/ui/states";
import { StatusLabel, type ProductStatus } from "@/components/admin/ui/status-label";
import { adminApi, ApiError } from "@/lib/admin/api";
import { LANGUAGES } from "@/lib/admin/dictionaries";
import { errorMessage } from "@/lib/admin/errors";
import {
  draftFrom,
  emptyDraft,
  hasErrors,
  isTranslationFilled,
  productKey,
  productListKey,
  sameDraft,
  toPayload,
  useProduct,
  validateDraft,
  type AdminProduct,
  type DraftErrors,
  type ProductDraft,
  type StatusTransition,
} from "@/lib/admin/product-editor";
import { DetailsSection, TranslationsSection } from "./details-sections";
import { ImagesSection } from "./images-section";
import { StepNav, type Step } from "./step-nav";
import { VariantsSection } from "./variants-section";

const TRANSITIONS: Record<ProductStatus, { transition: StatusTransition; label: string; primary?: boolean }[]> = {
  DRAFT: [
    { transition: "PUBLISH", label: "Publikuj", primary: true },
    { transition: "ARCHIVE", label: "Archiwizuj" },
  ],
  PUBLISHED: [{ transition: "HIDE", label: "Ukryj" }],
  HIDDEN: [
    { transition: "PUBLISH", label: "Publikuj ponownie", primary: true },
    { transition: "ARCHIVE", label: "Archiwizuj" },
  ],
  ARCHIVED: [{ transition: "RESTORE_TO_DRAFT", label: "Przywróć do szkicu" }],
};

const STATUS_DONE: Record<StatusTransition, string> = {
  PUBLISH: "Model opublikowany",
  HIDE: "Model ukryty",
  ARCHIVE: "Model zarchiwizowany",
  RESTORE_TO_DRAFT: "Model przywrócony do szkicu",
};

/** Kody błędów API, które dotyczą konkretnego pola formularza danych. */
const FIELD_OF_CODE: Record<string, keyof DraftErrors> = {
  "catalog.product.slug-taken": "slug",
  "catalog.product.slug": "slug",
  "catalog.product.style-code-taken": "styleCode",
  "catalog.product.style-code": "styleCode",
  "i18n.default-language-required": "name",
  "catalog.product.name": "name",
  "pricing.non-positive": "wholesale",
  "pricing.precision": "wholesale",
  "pricing.too-large": "wholesale",
};

function serverFieldErrors(error: unknown): DraftErrors {
  if (error instanceof ApiError && error.code && FIELD_OF_CODE[error.code]) {
    return { [FIELD_OF_CODE[error.code]]: errorMessage(error) } as DraftErrors;
  }
  return {};
}

function useUnsavedWarning(active: boolean) {
  useEffect(() => {
    if (!active) return;
    const warn = (event: BeforeUnloadEvent) => event.preventDefault();
    addEventListener("beforeunload", warn);
    return () => removeEventListener("beforeunload", warn);
  }, [active]);
}

export function ProductEditor({ productId }: { productId?: string }) {
  if (!productId) return <NewProductEditor />;
  return <ExistingProductLoader productId={productId} />;
}

function ExistingProductLoader({ productId }: { productId: string }) {
  const product = useProduct(productId);
  if (product.isPending) {
    return (
      <>
        <PageHeader title="Model" back={{ href: "/admin/products", label: "Modele" }} />
        <div className="px-8 py-8">
          <LoadingRows />
        </div>
      </>
    );
  }
  if (product.isError) {
    return (
      <>
        <PageHeader title="Model" back={{ href: "/admin/products", label: "Modele" }} />
        <div className="px-8 py-8">
          <ErrorState message={errorMessage(product.error)} onRetry={() => product.refetch()} />
        </div>
      </>
    );
  }
  return <ExistingProductEditor product={product.data} />;
}

function ExistingProductEditor({ product }: { product: AdminProduct }) {
  const router = useRouter();
  const client = useQueryClient();
  const flash = useFlash();
  const [baseline, setBaseline] = useState(() => draftFrom(product));
  const [draft, setDraft] = useState(baseline);
  const [syncedVersion, setSyncedVersion] = useState(product.version);
  const [showErrors, setShowErrors] = useState(false);
  const [serverErrors, setServerErrors] = useState<DraftErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [statusPending, setStatusPending] = useState(false);
  const [headerError, setHeaderError] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const dirty = !sameDraft(draft, baseline);

  // zmiany kolorów, wariantów i zdjęć podbijają wersję modelu — dane formularza pozostają, wzorzec się aktualizuje
  if (syncedVersion !== product.version) {
    setSyncedVersion(product.version);
    const fresh = draftFrom(product);
    setBaseline(fresh);
    if (!dirty) setDraft(fresh);
  }

  useUnsavedWarning(dirty);

  const errors = showErrors ? { ...validateDraft(draft), ...serverErrors } : serverErrors;

  const update = (change: (d: ProductDraft) => ProductDraft) => {
    setDraft(change);
    setServerErrors({});
    setFormError(null);
  };

  const refresh = async () => {
    await client.invalidateQueries({ queryKey: productKey(product.id) });
    void client.invalidateQueries({ queryKey: productListKey });
  };

  async function save(event: FormEvent) {
    event.preventDefault();
    setShowErrors(true);
    if (hasErrors(validateDraft(draft))) {
      setFormError("Popraw zaznaczone pola.");
      return;
    }
    setSaving(true);
    setFormError(null);
    try {
      await adminApi.put(`/products/${product.id}`, toPayload(draft, product.version));
      setBaseline(draft);
      setShowErrors(false);
      flash("Zapisano zmiany");
      await refresh();
    } catch (error) {
      setServerErrors(serverFieldErrors(error));
      setFormError(errorMessage(error));
      if (error instanceof ApiError && error.code === "data.concurrent-modification") await refresh();
    } finally {
      setSaving(false);
    }
  }

  async function changeStatus(transition: StatusTransition) {
    setStatusPending(true);
    setHeaderError(null);
    try {
      await adminApi.post(`/products/${product.id}/status`, { transition, version: product.version });
      flash(STATUS_DONE[transition]);
      await refresh();
    } catch (error) {
      setHeaderError(errorMessage(error));
      if (error instanceof ApiError && error.code === "data.concurrent-modification") await refresh();
    } finally {
      setStatusPending(false);
    }
  }

  async function remove() {
    setStatusPending(true);
    try {
      await adminApi.delete(`/products/${product.id}`);
      client.removeQueries({ queryKey: productKey(product.id) });
      void client.invalidateQueries({ queryKey: productListKey });
      flash("Usunięto model");
      router.push("/admin/products");
    } catch (error) {
      setHeaderError(errorMessage(error));
      setConfirmDelete(false);
      setStatusPending(false);
    }
  }

  const missingTranslations = LANGUAGES.filter((l) => l !== "pl" && !isTranslationFilled(draft.translations[l])).length;
  const steps: Step[] = [
    { id: "dane", label: "Dane" },
    {
      id: "tlumaczenia",
      label: "Tłumaczenia",
      note: missingTranslations === 0 ? "komplet" : `brak ${missingTranslations} z 3`,
      attention: missingTranslations > 0,
    },
    {
      id: "warianty",
      label: "Kolory i rozmiary",
      note: product.variants.length === 0 ? "brak wariantów" : plural(product.variants.length, "wariant", "warianty", "wariantów"),
      attention: product.variants.length === 0,
    },
    {
      id: "zdjecia",
      label: "Zdjęcia",
      note: product.images.length === 0 ? "brak zdjęć" : plural(product.images.length, "zdjęcie", "zdjęcia", "zdjęć"),
      attention: product.images.length === 0,
    },
  ];

  const name = baseline.translations.pl.name || "Model bez nazwy";

  return (
    <>
      <PageHeader
        back={{ href: "/admin/products", label: "Modele" }}
        title={name}
        meta={
          <span className="inline-flex items-center gap-3">
            <span className="tabular">{product.styleCode}</span>
            <StatusLabel status={product.status} />
          </span>
        }
        actions={
          <>
            {product.status === "PUBLISHED" && (
              <a
                href={`/p/${product.slug}`}
                target="_blank"
                rel="noopener"
                className="inline-flex h-10 items-center gap-2 px-3 text-ink-2 hover:text-foreground"
              >
                <ExternalLink aria-hidden className="size-4" strokeWidth={1.5} />
                Zobacz w katalogu
              </a>
            )}
            <Button variant="danger" onClick={() => setConfirmDelete(true)} disabled={statusPending}>
              Usuń
            </Button>
            {TRANSITIONS[product.status].map((action) => (
              <Button
                key={action.transition}
                variant={action.primary && !dirty ? "primary" : "secondary"}
                disabled={statusPending || dirty}
                title={dirty ? "Najpierw zapisz lub odrzuć zmiany" : undefined}
                onClick={() => changeStatus(action.transition)}
              >
                {action.label}
              </Button>
            ))}
          </>
        }
      />
      {headerError && (
        <p role="alert" className="border-b border-line px-8 py-3 text-small text-danger">
          {headerError}
        </p>
      )}

      <div className="grid max-w-[1400px] gap-12 px-8 py-8 lg:grid-cols-[180px_minmax(0,1fr)]">
        <StepNav steps={steps} />
        <div className="min-w-0">
          <form id="product-form" onSubmit={save} noValidate>
            <DetailsSection draft={draft} errors={errors} onChange={update} />
            <TranslationsSection draft={draft} errors={errors} onChange={update} />
          </form>
          <VariantsSection product={product} />
          <ImagesSection product={product} />
        </div>
      </div>

      {(dirty || formError) && (
        <SaveBar
          form="product-form"
          message={formError ?? "Niezapisane zmiany w danych lub tłumaczeniach."}
          error={Boolean(formError)}
          saving={saving}
          primaryLabel="Zapisz zmiany"
          onDiscard={() => {
            setDraft(baseline);
            setShowErrors(false);
            setServerErrors({});
            setFormError(null);
          }}
        />
      )}

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title={`Usunąć model „${name}”?`}
        confirmLabel="Usuń model"
        pending={statusPending}
        onConfirm={remove}
      >
        Model zniknie z katalogu i panelu razem z wariantami i zdjęciami. Wysłane już zestawienia zachowują jego dane.
        Jeśli model ma wrócić w przyszłym sezonie, lepiej go zarchiwizować.
      </ConfirmDialog>
    </>
  );
}

function NewProductEditor() {
  const router = useRouter();
  const client = useQueryClient();
  const flash = useFlash();
  const [draft, setDraft] = useState<ProductDraft>(emptyDraft);
  const [showErrors, setShowErrors] = useState(false);
  const [serverErrors, setServerErrors] = useState<DraftErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useUnsavedWarning(!sameDraft(draft, emptyDraft()) && !saving);

  const errors = showErrors ? { ...validateDraft(draft), ...serverErrors } : serverErrors;
  const update = (change: (d: ProductDraft) => ProductDraft) => {
    setDraft(change);
    setServerErrors({});
    setFormError(null);
  };

  async function create(event: FormEvent) {
    event.preventDefault();
    setShowErrors(true);
    if (hasErrors(validateDraft(draft))) {
      setFormError("Popraw zaznaczone pola.");
      return;
    }
    setSaving(true);
    setFormError(null);
    try {
      const { id } = await adminApi.post<{ id: string }>("/products", toPayload(draft));
      void client.invalidateQueries({ queryKey: productListKey });
      flash("Utworzono model — dodaj kolory, rozmiary i zdjęcia");
      router.replace(`/admin/products/${id}#warianty`);
    } catch (error) {
      setServerErrors(serverFieldErrors(error));
      setFormError(errorMessage(error));
      setSaving(false);
    }
  }

  return (
    <>
      <PageHeader back={{ href: "/admin/products", label: "Modele" }} title="Nowy model" meta="Szkic" />
      <div className="grid max-w-[1400px] gap-12 px-8 py-8 lg:grid-cols-[180px_minmax(0,1fr)]">
        <StepNav
          steps={[
            { id: "dane", label: "Dane" },
            { id: "tlumaczenia", label: "Tłumaczenia" },
            { id: "warianty-info", label: "Kolory, rozmiary, zdjęcia", note: "po utworzeniu" },
          ]}
        />
        <div className="min-w-0">
          <form id="product-form" onSubmit={create} noValidate>
            <DetailsSection draft={draft} errors={errors} onChange={update} />
            <TranslationsSection draft={draft} errors={errors} onChange={update} />
          </form>
          <section id="warianty-info" className="border-t border-line py-10">
            <h2 className="text-body font-medium">Kolory, rozmiary i zdjęcia</h2>
            <p className="mt-1 max-w-[70ch] text-ink-3">
              Dostępne po utworzeniu modelu — najpierw zapisz dane, potem dodasz warianty z kodami EAN i wgrasz zdjęcia.
            </p>
          </section>
        </div>
      </div>
      <SaveBar
        form="product-form"
        message={formError ?? "Model powstanie jako szkic — opublikujesz go po dodaniu rozmiarów."}
        error={Boolean(formError)}
        saving={saving}
        primaryLabel="Utwórz model"
        savingLabel="Tworzenie…"
        onDiscard={() => router.push("/admin/products")}
        discardLabel="Anuluj"
      />
    </>
  );
}

function SaveBar({
  form,
  message,
  error,
  saving,
  primaryLabel,
  savingLabel = "Zapisywanie…",
  onDiscard,
  discardLabel = "Odrzuć",
}: {
  form: string;
  message: string;
  error: boolean;
  saving: boolean;
  primaryLabel: string;
  savingLabel?: string;
  onDiscard: () => void;
  discardLabel?: string;
}) {
  return (
    <div className="sticky bottom-0 z-20 border-t border-line bg-background animate-in slide-in-from-bottom-2 fade-in-0 duration-300">
      <div className="flex max-w-[1400px] flex-wrap items-center gap-4 px-8 py-3 lg:pl-[calc(2rem+180px+3rem)]">
        <p role={error ? "alert" : "status"} className={error ? "flex-1 text-small text-danger" : "flex-1 text-small text-ink-2"}>
          {message}
        </p>
        <Button variant="text" onClick={onDiscard} disabled={saving}>
          {discardLabel}
        </Button>
        <Button type="submit" form={form} disabled={saving}>
          {saving ? savingLabel : primaryLabel}
        </Button>
      </div>
    </div>
  );
}
