"use client"

import { Dialog } from "@base-ui/react/dialog"
import * as React from "react"
import { z } from "zod"
import { Button } from "@/app/components/ui/button"
import { ApiError, getErrorMessage } from "@/services/api"

/** Giới hạn khớp backend (ReasonRules): bắt buộc, cắt khoảng trắng hai đầu rồi tối đa 1000 ký tự. */
export const REASON_MAX = 1000
const reasonSchema = z.string().trim().min(1, "Vui lòng nhập lý do").max(REASON_MAX, `Lý do tối đa ${REASON_MAX} ký tự`)

interface ReasonDialogProps {
  title: string
  description: string
  confirmLabel: string
  /** Gửi lý do đã trim. Ném lỗi thì hộp giữ nguyên và hiện lỗi; xong thì bên gọi tự đóng hộp. */
  onConfirm: (reason: string) => Promise<void>
  onClose: () => void
}

/** Lỗi theo trường `errors.reason` của backend (400 VALIDATION_ERROR), nếu có. */
function reasonFieldError(error: unknown): string | null {
  if (!(error instanceof ApiError) || !error.errors || typeof error.errors !== "object") return null
  const message = (error.errors as Record<string, unknown>).reason
  return typeof message === "string" ? message : null
}

/** Hộp nhập lý do bắt buộc (trả lại / đóng JD, khoá công ty); chỉ render khi mở nên đóng = form tự reset. */
export function ReasonDialog({ title, description, confirmLabel, onConfirm, onClose }: ReasonDialogProps) {
  const [reason, setReason] = React.useState("")
  const [fieldError, setFieldError] = React.useState("")
  const [formError, setFormError] = React.useState("")
  const [submitting, setSubmitting] = React.useState(false)
  const lock = React.useRef(false)
  const id = React.useId()
  const length = reason.trim().length

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (lock.current) return
    const parsed = reasonSchema.safeParse(reason)
    if (!parsed.success) {
      setFieldError(parsed.error.issues[0].message)
      return
    }
    lock.current = true
    setSubmitting(true)
    setFieldError("")
    setFormError("")
    try {
      await onConfirm(parsed.data)
    } catch (error) {
      const field = reasonFieldError(error)
      if (field) setFieldError(field)
      else setFormError(getErrorMessage(error))
    } finally {
      lock.current = false
      setSubmitting(false)
    }
  }

  return (
    <Dialog.Root
      open
      onOpenChange={(value) => {
        if (!value && !submitting) onClose()
      }}
    >
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/45" />
        <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-2xl border bg-background p-6 shadow-xl">
          <Dialog.Title className="text-base font-semibold">{title}</Dialog.Title>
          <Dialog.Description className="mb-4 mt-2 text-xs/relaxed text-muted-foreground">{description}</Dialog.Description>
          <form onSubmit={submit} noValidate className="space-y-3">
            <label htmlFor={`${id}-reason`} className="block text-xs font-medium">
              Lý do <span className="text-destructive">*</span>
            </label>
            <textarea
              id={`${id}-reason`}
              rows={5}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              disabled={submitting}
              aria-invalid={fieldError ? true : undefined}
              aria-describedby={`${id}-hint${fieldError ? ` ${id}-error` : ""}`}
              className="w-full rounded-md border border-input bg-background p-2 text-xs aria-invalid:border-destructive"
            />
            <div className="flex items-start justify-between gap-2 text-xs">
              {fieldError ? (
                <p id={`${id}-error`} role="alert" className="text-destructive">
                  {fieldError}
                </p>
              ) : (
                <span />
              )}
              <span id={`${id}-hint`} className={length > REASON_MAX ? "shrink-0 text-destructive" : "shrink-0 text-muted-foreground"}>
                {length}/{REASON_MAX}
              </span>
            </div>
            {formError ? (
              <p role="alert" className="text-xs text-destructive">
                {formError}
              </p>
            ) : null}
            <div className="flex justify-end gap-2">
              <Dialog.Close disabled={submitting} render={<Button type="button" variant="outline" />}>
                Huỷ
              </Dialog.Close>
              <Button type="submit" variant="destructive" disabled={submitting}>
                {submitting ? "Đang xử lý..." : confirmLabel}
              </Button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
