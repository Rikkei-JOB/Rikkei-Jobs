import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "cn"

const alertVariants = cva("relative w-full rounded-xl border px-4 py-3 text-sm leading-relaxed", {
  variants: {
    variant: {
      default: "border-[#BFDBFE] bg-[#EFF6FF] text-[#1E3A8A]",
      destructive: "border-[#FECACA] bg-[#FEF2F2] text-[#B91C1C]",
      success: "border-[#BBF7D0] bg-[#F0FDF4] text-[#166534]",
      warning: "border-[#FDE68A] bg-[#FFFBEB] text-[#92400E]",
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
