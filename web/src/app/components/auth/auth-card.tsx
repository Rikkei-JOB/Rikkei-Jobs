import * as React from "react"
import { GraduationCap, type LucideIcon } from "lucide-react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card"

interface AuthCardProps {
  title: string
  description?: string
  children: React.ReactNode
  footer?: React.ReactNode
  /** Biểu tượng đầu thẻ; mặc định mũ tốt nghiệp. */
  icon?: LucideIcon
}

export function AuthCard({ title, description, children, footer, icon: Icon = GraduationCap }: AuthCardProps) {
  return (
    <Card className="relative w-full max-w-[440px] overflow-hidden rounded-2xl border border-border bg-white shadow-[0_16px_48px_rgb(15_23_42/0.10)]">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-28 bg-[radial-gradient(ellipse_at_top,#FFE4E6_0%,transparent_72%)]"
      />
      <CardHeader className="relative justify-items-center pt-2 text-center">
        <div className="mb-2 flex size-12 items-center justify-center rounded-2xl border border-[#FECDD3] bg-[#FFF1F2] text-[#DA251D] shadow-2xs">
          <Icon className="size-6" aria-hidden="true" />
        </div>
        <CardTitle className="text-xl font-extrabold text-heading">{title}</CardTitle>
        {description ? <CardDescription className="max-w-xs text-center text-pretty">{description}</CardDescription> : null}
      </CardHeader>
      <CardContent className="relative space-y-4">{children}</CardContent>
      {footer ? (
        <div className="relative border-t border-border/70 px-(--card-spacing) pt-4 text-center text-sm text-muted-foreground">
          {footer}
        </div>
      ) : null}
    </Card>
  )
}
