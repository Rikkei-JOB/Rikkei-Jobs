import { Button as ButtonPrimitive } from "@base-ui/react/button"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "cn"

const buttonVariants = cva(
  "group/button inline-flex shrink-0 cursor-pointer items-center justify-center rounded-lg border border-transparent bg-clip-padding text-sm font-semibold whitespace-nowrap transition-all duration-150 outline-none select-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 active:not-aria-[haspopup]:translate-y-px disabled:pointer-events-none disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-2 aria-invalid:ring-destructive/20 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground shadow-2xs hover:bg-[#941C25]",
        outline:
          "border-[#CBD5E1] bg-white text-[#051A53] shadow-2xs hover:border-[#B4232D]/40 hover:bg-[#FFF1F2] hover:text-[#B4232D] aria-expanded:bg-accent aria-expanded:text-accent-foreground",
        secondary:
          "bg-[#F1F5F9] text-[#051A53] hover:bg-[#E2E8F0] aria-expanded:bg-secondary aria-expanded:text-secondary-foreground",
        ghost:
          "text-[#334155] hover:bg-[#FFF1F2] hover:text-[#B4232D] aria-expanded:bg-accent aria-expanded:text-accent-foreground",
        destructive:
          "bg-[#FEF2F2] border-[#FECACA] text-[#B91C1C] hover:bg-[#FEE2E2] focus-visible:border-destructive/40 focus-visible:ring-destructive/20",
        link: "text-primary underline-offset-4 hover:underline",
      },
      size: {
        default:
          "min-h-10 gap-2 px-4 py-2 text-sm has-data-[icon=inline-end]:pr-3 has-data-[icon=inline-start]:pl-3",
        xs: "min-h-7 gap-1 rounded-md px-2.5 py-1 text-xs [&_svg:not([class*='size-'])]:size-3.5",
        sm: "min-h-9 gap-1.5 rounded-lg px-3.5 py-1.5 text-xs font-semibold [&_svg:not([class*='size-'])]:size-3.5",
        lg: "min-h-11 gap-2 rounded-xl px-6 py-2.5 text-sm font-bold [&_svg:not([class*='size-'])]:size-4",
        icon: "size-10 [&_svg:not([class*='size-'])]:size-4",
        "icon-xs": "size-7 rounded-md [&_svg:not([class*='size-'])]:size-3.5",
        "icon-sm": "size-9 rounded-lg [&_svg:not([class*='size-'])]:size-4",
        "icon-lg": "size-11 rounded-xl [&_svg:not([class*='size-'])]:size-5",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function Button({
  className,
  variant = "default",
  size = "default",
  ...props
}: ButtonPrimitive.Props & VariantProps<typeof buttonVariants>) {
  return (
    <ButtonPrimitive
      data-slot="button"
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
