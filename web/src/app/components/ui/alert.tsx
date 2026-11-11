import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "cn"

const alertVariants = cva("relative w-full rounded-lg border px-3 py-2 text-xs/relaxed", {
  variants: {
    variant: {
      default: "border-border bg-muted/50 text-foreground",
      destructive: "border-destructive/30 bg-destructive/10 text-destructive",
      success: "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
    },
  },
  defaultVariants: {
    variant: "default",
  },
})

function Alert({
  className,
  variant,
  ...props
}: React.ComponentProps<"div"> & VariantProps<typeof alertVariants>) {
  return (
    <div
      data-slot="alert"
      role={variant === "destructive" ? "alert" : "status"}
      className={cn(alertVariants({ variant }), className)}
      {...props}
    />
  )
}

export { Alert }
