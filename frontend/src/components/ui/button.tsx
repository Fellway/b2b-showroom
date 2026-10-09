import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { Slot } from "radix-ui"

import { cn } from "@/lib/utils"

/**
 * Przyciski systemu (DESIGN.md › Buttons): płaskie, ostre narożniki, bez cieni i bez przesunięć.
 * - primary: jedna czarna akcja na widok,
 * - secondary: akcja obok głównej — obrys `edge`,
 * - text: akcje pomocnicze tekstem,
 * - danger: akcja nieodwracalna (tekst w `danger`, tylko po potwierdzeniu),
 * - icon: kwadrat z ikoną Lucide.
 */
const buttonVariants = cva(
  "inline-flex shrink-0 items-center justify-center gap-2 whitespace-nowrap text-small transition-colors outline-none disabled:pointer-events-none [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        primary: "bg-foreground font-medium text-background hover:bg-ink-2 disabled:bg-well disabled:text-ink-3",
        secondary: "border border-edge bg-background text-foreground hover:border-foreground disabled:border-line disabled:text-ink-3",
        text: "text-ink-2 hover:text-foreground disabled:text-ink-3",
        danger: "text-danger underline-offset-4 hover:underline disabled:text-ink-3",
        icon: "text-ink-3 hover:text-foreground disabled:text-line",
      },
      size: {
        lg: "h-12 px-6",
        md: "h-10 px-4",
        sm: "h-8 px-3 text-meta",
        icon: "size-9",
        "icon-sm": "size-8",
      },
    },
    compoundVariants: [
      { variant: "text", size: ["lg", "md", "sm"], className: "px-3" },
      { variant: "danger", size: ["lg", "md", "sm"], className: "px-3" },
    ],
    defaultVariants: {
      variant: "primary",
      size: "md",
    },
  }
)

function Button({
  className,
  variant,
  size,
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
  }) {
  const Comp = asChild ? Slot.Root : "button"

  return (
    <Comp
      data-slot="button"
      data-variant={variant}
      data-size={size}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
