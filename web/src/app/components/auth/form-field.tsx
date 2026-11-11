import * as React from "react"
import { Label } from "@/app/components/ui/label"

interface FormFieldProps {
  label: string
  htmlFor: string
  error?: string
  children: React.ReactNode
}

/** Nhãn + ô nhập + thông báo lỗi dưới ô nhập. */
export function FormField({ label, htmlFor, error, children }: FormFieldProps) {
  return (
    <div className="space-y-1.5">
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
      {error ? (
        <p id={`${htmlFor}-error`} role="alert" className="text-xs/relaxed text-destructive">
          {error}
        </p>
      ) : null}
    </div>
  )
}
