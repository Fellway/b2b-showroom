"use client";

import Image from "next/image";
import { useSearchParams } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { ArrowLeft, ArrowRight, Check, Minus, Plus } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, usePathname, useRouter } from "@/i18n/navigation";
import { track } from "@/lib/analytics";
import { formatMarkup, formatMoney } from "@/lib/format";
import type { ColorOption, ProductDetail, SalesRepresentative } from "@/lib/types";
import { useHydrated } from "@/lib/use-hydrated";
import { cn } from "@/lib/utils";
import { useSelection, type SelectionLine } from "@/store/selection";
import { RepresentativeContact } from "@/components/site/representative-contact";

type Picked = Record<string, number | null>;

export function ProductView({
  product,
  representatives,
}: {
  product: ProductDetail;
  representatives: SalesRepresentative[];
}) {
  const t = useTranslations("product");
  const tSite = useTranslations("site");
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const hydrated = useHydrated();

  const initialColor =
    product.colors.find((c) => c.code === searchParams.get("color")) ?? product.colors[0];
  const [colorId, setColorId] = useState(initialColor?.id);
  const color = product.colors.find((c) => c.id === colorId) ?? product.colors[0];

  const lines = useSelection((s) => s.lines);
  const replaceColor = useSelection((s) => s.replaceColor);
  const setDrawerOpen = useSelection((s) => s.setDrawerOpen);

  const savedForColor = useMemo(() => {
    const saved: Picked = {};
    for (const line of lines) {
      if (line.productId === product.id && line.colorId === color?.id) saved[line.variantId] = line.quantity;
    }
    return saved;
  }, [lines, product.id, color?.id]);

  const [picked, setPicked] = useState<Picked>({});
  const [justAdded, setJustAdded] = useState(false);

  // po hydratacji i przy zmianie koloru formularz pokazuje to, co już jest w zestawieniu
  // (stan wyprowadzany podczas renderu zamiast efektu — bez dodatkowego cyklu renderowania)
  const syncKey = `${hydrated}:${color?.id}`;
  const [syncedKey, setSyncedKey] = useState(syncKey);
  if (syncedKey !== syncKey) {
    setSyncedKey(syncKey);
    setPicked(savedForColor);
    setJustAdded(false);
  }

  useEffect(() => {
    track({ type: "PRODUCT_VIEW", language: locale, productId: product.id });
  }, [product.id, locale]);

  if (!color) return null;

  const images = galleryFor(product, color);
  const pickedIds = Object.keys(picked);
  const savedIds = Object.keys(savedForColor);
  const changed =
    pickedIds.length !== savedIds.length ||
    pickedIds.some((id) => !(id in savedForColor) || savedForColor[id] !== picked[id]);
  const updating = savedIds.length > 0;

  function chooseColor(next: ColorOption) {
    setColorId(next.id);
    setJustAdded(false);
    const params = new URLSearchParams(searchParams);
    params.set("color", next.code);
    router.replace(`${pathname}?${params}`, { scroll: false });
  }

  function toggleSize(variantId: string) {
    setJustAdded(false);
    setPicked((current) => {
      const next = { ...current };
      if (variantId in next) delete next[variantId];
      else next[variantId] = null;
      return next;
    });
  }

  function setQuantity(variantId: string, quantity: number | null) {
    setJustAdded(false);
    setPicked((current) => ({ ...current, [variantId]: quantity }));
  }

  function commit() {
    const cover = images[0]?.url ?? null;
    const next: SelectionLine[] = color.variants
      .map((variant, index) => ({ variant, index }))
      .filter(({ variant }) => variant.id in picked)
      .map(({ variant, index }) => ({
        variantId: variant.id,
        productId: product.id,
        slug: product.slug,
        styleCode: product.styleCode,
        name: product.name,
        colorId: color.id,
        colorName: color.name,
        colorHex: color.hex,
        size: variant.size,
        sizeOrder: index,
        ean: variant.ean,
        imageUrl: cover,
        wholesale: product.price.wholesale,
        retail: product.price.retail,
        currency: product.price.currency,
        quantity: picked[variant.id] ?? null,
      }));
    for (const id of pickedIds.filter((id) => !(id in savedForColor))) {
      track({ type: "SELECTION_ADDED", language: locale, productId: product.id, variantId: id });
    }
    for (const id of savedIds.filter((id) => !(id in picked))) {
      track({ type: "SELECTION_REMOVED", language: locale, productId: product.id, variantId: id });
    }
    replaceColor(product.id, color.id, next);
    setJustAdded(true);
  }

  const allPicked = color.variants.length > 0 && color.variants.every((v) => v.id in picked);
  const savedSizes = color.variants.filter((v) => v.id in savedForColor).map((v) => v.size);

  return (
    <div className="mx-auto max-w-[1600px] md:px-8 md:pt-8 lg:grid lg:grid-cols-12 lg:gap-12">
      <Gallery images={images} productName={product.name} colorName={color.name} />

      <section className="px-5 pt-6 md:px-0 lg:sticky lg:top-[calc(var(--header-height)+2rem)] lg:col-span-5 lg:self-start lg:pt-0">
        <Link href="/" className="inline-flex items-center gap-1.5 text-small text-ink-3 hover:text-foreground">
          <ArrowLeft aria-hidden className="size-4" strokeWidth={1.5} />
          {t("back")}
        </Link>

        <h1 className="mt-6 text-title font-medium text-balance md:text-[1.75rem] md:leading-[2.25rem]">{product.name}</h1>
        <p className="mt-1 text-small text-ink-3">
          {t("styleCode", { code: product.styleCode })} · {product.category.name}
        </p>

        <dl className="tabular mt-6 grid grid-cols-[auto_1fr] items-baseline gap-x-6 gap-y-1.5 border-y border-line py-4 text-small">
          <dt className="text-ink-3">{t("wholesale")}</dt>
          <dd className="text-right text-body font-medium">
            {formatMoney(product.price.wholesale, product.price.currency, locale)}
          </dd>
          <dt className="text-ink-3">{t("retail")}</dt>
          <dd className="text-right">{formatMoney(product.price.retail, product.price.currency, locale)}</dd>
          <dt className="text-ink-3">{t("markup")}</dt>
          <dd className="text-right">{formatMarkup(product.price.markup, locale)}</dd>
        </dl>

        <fieldset className="mt-6">
          <legend className="text-small">
            <span className="text-ink-3">{t("color")}: </span>
            {color.name}
          </legend>
          <div className="mt-3 flex flex-wrap gap-2">
            {product.colors.map((option) => {
              const active = option.id === color.id;
              const inSelection = lines.some((l) => l.productId === product.id && l.colorId === option.id);
              return (
                <button
                  key={option.id}
                  type="button"
                  aria-pressed={active}
                  aria-label={option.name}
                  title={option.name}
                  onClick={() => chooseColor(option)}
                  className={cn(
                    "relative size-10 border p-1 transition-colors",
                    active ? "border-foreground" : "border-edge hover:border-foreground",
                  )}
                >
                  <span className="block size-full border border-black/10" style={{ backgroundColor: option.hex ?? "transparent" }} />
                  {hydrated && inSelection && (
                    <span aria-hidden className="absolute -right-1 -top-1 size-2.5 rounded-full border-2 border-background bg-foreground" />
                  )}
                </button>
              );
            })}
          </div>
        </fieldset>

        <fieldset className="mt-6">
          <div className="flex items-baseline justify-between">
            <legend className="text-small text-ink-3">{t("sizes")}</legend>
            <button
              type="button"
              onClick={() => {
                setJustAdded(false);
                setPicked(allPicked ? {} : Object.fromEntries(color.variants.map((v) => [v.id, picked[v.id] ?? null])));
              }}
              className="text-meta underline underline-offset-4 hover:no-underline"
            >
              {allPicked ? t("noneSizes") : t("allSizes")}
            </button>
          </div>
          <div className="mt-3 grid grid-cols-6 gap-1.5">
            {color.variants.map((variant) => {
              const active = variant.id in picked;
              return (
                <button
                  key={variant.id}
                  type="button"
                  aria-pressed={active}
                  onClick={() => toggleSize(variant.id)}
                  className={cn(
                    "tabular flex h-11 items-center justify-center border text-small transition-colors",
                    active ? "border-foreground bg-foreground text-background" : "border-edge hover:border-foreground",
                  )}
                >
                  {variant.size}
                </button>
              );
            })}
          </div>

          {pickedIds.length > 0 && (
            <div className="mt-4 border-t border-line pt-3">
              <p className="text-meta text-ink-3">{t("quantityHint")}</p>
              <ul className="mt-2 flex flex-col">
                {color.variants
                  .filter((v) => v.id in picked)
                  .map((variant) => (
                    <li key={variant.id} className="flex items-center justify-between py-1.5">
                      <span className="tabular w-12 text-small">{variant.size}</span>
                      <QuantityStepper
                        value={picked[variant.id]}
                        label={t("quantity", { size: variant.size })}
                        onChange={(q) => setQuantity(variant.id, q)}
                      />
                    </li>
                  ))}
              </ul>
            </div>
          )}
        </fieldset>

        <div className="mt-6 flex flex-col gap-3">
          <button
            type="button"
            onClick={commit}
            disabled={!changed}
            className={cn(
              "flex h-12 items-center justify-center gap-2 px-6 text-small font-medium transition-colors",
              "bg-foreground text-background hover:bg-ink-2 disabled:cursor-not-allowed",
              !changed && !justAdded && "disabled:bg-well disabled:text-ink-3",
              justAdded && "disabled:bg-foreground disabled:text-background",
            )}
          >
            {justAdded ? (
              <>
                <Check aria-hidden className="size-4 animate-in zoom-in-50 duration-300" strokeWidth={2} />
                {t("added")}
              </>
            ) : updating && changed ? (
              t("update")
            ) : (
              t("add", { count: pickedIds.length })
            )}
          </button>
          {hydrated && savedSizes.length > 0 && (
            <p className="flex items-center justify-between gap-4 text-meta text-ink-3" aria-live="polite">
              <span className="tabular">{t("inSelection", { sizes: savedSizes.join(", ") })}</span>
              <button
                type="button"
                onClick={() => setDrawerOpen(true)}
                className="inline-flex shrink-0 items-center gap-1 text-foreground underline underline-offset-4 hover:no-underline"
              >
                {tSite("selection")}
                <ArrowRight aria-hidden className="size-3.5" strokeWidth={1.5} />
              </button>
            </p>
          )}
        </div>

        <div className="mt-10 flex flex-col divide-y divide-line border-y border-line">
          {product.description && (
            <Detail title={t("description")} open>
              <p className="max-w-[62ch] whitespace-pre-line text-ink-2">{product.description}</p>
            </Detail>
          )}
          {product.materialComposition && (
            <Detail title={t("composition")}>
              <p className="text-ink-2">{product.materialComposition}</p>
            </Detail>
          )}
          {product.careInstructions && (
            <Detail title={t("care")}>
              <p className="whitespace-pre-line text-ink-2">{product.careInstructions}</p>
            </Detail>
          )}
          <Detail title={t("codes")}>
            <table className="tabular w-full text-small">
              <thead>
                <tr className="text-left text-meta text-ink-3">
                  <th className="pb-2 font-normal">{t("codesSize")}</th>
                  <th className="pb-2 font-normal">EAN</th>
                  <th className="pb-2 font-normal">SKU</th>
                </tr>
              </thead>
              <tbody>
                {color.variants.map((variant) => (
                  <tr key={variant.id} className="border-t border-line">
                    <td className="py-1.5">{variant.size}</td>
                    <td className="py-1.5 select-all">{variant.ean}</td>
                    <td className="py-1.5 text-ink-2">{variant.sku}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Detail>
        </div>

        {representatives.length > 0 && (
          <div className="mt-8 bg-well p-5">
            <p className="text-small text-ink-2">{tSite("footer.contactLead")}</p>
            <div className="mt-4 flex flex-col gap-5">
              {representatives.map((rep) => (
                <RepresentativeContact key={rep.id} representative={rep} />
              ))}
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

function galleryFor(product: ProductDetail, color: ColorOption) {
  const forColor = product.images.filter((image) => image.colorId === color.id || image.colorId === null);
  return forColor.length > 0 ? forColor : product.images;
}

function Gallery({
  images,
  productName,
  colorName,
}: {
  images: ProductDetail["images"];
  productName: string;
  colorName: string;
}) {
  const t = useTranslations("product");
  if (images.length === 0) {
    return <div className="aspect-[2/3] bg-well lg:col-span-7" />;
  }
  return (
    <div
      className={cn(
        "flex snap-x snap-mandatory overflow-x-auto [scrollbar-width:none] lg:col-span-7 lg:grid lg:gap-2 lg:overflow-visible",
        images.length > 1 ? "lg:grid-cols-2" : "lg:grid-cols-1",
      )}
    >
      {images.map((image, index) => (
        <figure
          key={image.id}
          className={cn(
            "relative aspect-[2/3] shrink-0 snap-start bg-well lg:w-auto",
            images.length > 1 ? "w-[86vw] sm:w-[60vw]" : "w-full sm:mx-auto sm:w-[70vw] lg:mx-0 lg:w-full",
            images.length > 1 && index === 0 && images.length % 2 === 1 && "lg:col-span-2 lg:mx-auto lg:w-2/3",
          )}
        >
          <Image
            src={image.url}
            alt={t("image", { name: productName, color: colorName, index: index + 1 })}
            fill
            priority={index < 2}
            sizes="(min-width: 1024px) 30vw, 86vw"
            className="object-cover"
          />
        </figure>
      ))}
    </div>
  );
}

function Detail({ title, open, children }: { title: string; open?: boolean; children: React.ReactNode }) {
  return (
    <details open={open} className="group py-4">
      <summary className="flex cursor-pointer list-none items-center justify-between text-small marker:hidden [&::-webkit-details-marker]:hidden">
        {title}
        <Plus aria-hidden className="size-4 transition-transform duration-300 group-open:rotate-45" strokeWidth={1.5} />
      </summary>
      <div className="pt-3">{children}</div>
    </details>
  );
}

function QuantityStepper({
  value,
  label,
  onChange,
}: {
  value: number | null | undefined;
  label: string;
  onChange: (value: number | null) => void;
}) {
  const current = value ?? 0;
  const set = (next: number) => onChange(next <= 0 ? null : Math.min(next, 9999));
  return (
    <div className="flex items-center border border-edge">
      <button
        type="button"
        aria-label={`${label} −`}
        onClick={() => set(current - 1)}
        disabled={current === 0}
        className="flex size-9 items-center justify-center text-ink-2 hover:text-foreground disabled:text-line"
      >
        <Minus aria-hidden className="size-3.5" strokeWidth={1.5} />
      </button>
      <input
        type="number"
        inputMode="numeric"
        min={0}
        max={9999}
        aria-label={label}
        value={value ?? ""}
        placeholder=""
        onChange={(e) => {
          const parsed = Number.parseInt(e.target.value, 10);
          onChange(Number.isFinite(parsed) && parsed > 0 ? Math.min(parsed, 9999) : null);
        }}
        className="tabular h-9 w-12 border-x border-line bg-transparent text-center text-small outline-none [appearance:textfield] focus-visible:bg-well [&::-webkit-inner-spin-button]:appearance-none"
      />
      <button
        type="button"
        aria-label={`${label} +`}
        onClick={() => set(current + 1)}
        className="flex size-9 items-center justify-center text-ink-2 hover:text-foreground"
      >
        <Plus aria-hidden className="size-3.5" strokeWidth={1.5} />
      </button>
    </div>
  );
}
