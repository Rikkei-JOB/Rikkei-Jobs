"use client"

import { Dialog } from "@base-ui/react/dialog"
import * as React from "react"
import { Button } from "@/app/components/ui/button"
import { Input } from "@/app/components/ui/input"
import { useCreateHr } from "@/hooks/use-staff-companies"
import { addHrSchema } from "@/schemas/company.schema"
import { ApiError } from "@/services/api"
import { hrErrorMessage } from "./hr-error"

/** Hộp thêm tài khoản HR; chỉ render khi mở (đóng = unmount nên form tự reset). */
export function AddHrDialog({ companyId, onClose, onDone }: { companyId: string; onClose: () => void; onDone: (message: string) => void }) {
  const create = useCreateHr(companyId)
  const [email, setEmail] = React.useState("")
  const [fullName, setFullName] = React.useState("")
  const [error, setError] = React.useState("")

  function submit(event: React.FormEvent) {
    event.preventDefault()
    if (create.isPending) return
    const parsed = addHrSchema.safeParse({ email, fullName })
    if (!parsed.success) {
      setError(parsed.error.issues[0].message)
      return
    }
    setError("")
    create.mutate(parsed.data, {
      onSuccess: () => {
        onDone("Đã tạo tài khoản và gửi email mật khẩu tạm")
        onClose()
      },
      onError: (err) => setError(err instanceof ApiError && err.status === 409 ? "Email đã được sử dụng" : hrErrorMessage(err)),
    })
  }

  return (
    <Dialog.Root
      open
      onOpenChange={(value) => {
        if (!value && !create.isPending) onClose()
      }}
    >
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/45" />
        <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-2xl border bg-background p-6 shadow-xl">
          <Dialog.Title className="text-base font-semibold">Thêm tài khoản HR</Dialog.Title>
          <Dialog.Description className="mb-4 mt-2 text-xs/relaxed text-muted-foreground">
            Hệ thống tạo tài khoản và gửi mật khẩu tạm qua email.
          </Dialog.Description>
          <form onSubmit={submit} noValidate className="space-y-3">
            <div className="space-y-1">
              <label htmlFor="add-hr-email" className="block text-xs font-medium">
                Email
              </label>
              <Input id="add-hr-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} disabled={create.isPending} />
            </div>
            <div className="space-y-1">
              <label htmlFor="add-hr-name" className="block text-xs font-medium">
                Họ tên
              </label>
              <Input id="add-hr-name" value={fullName} onChange={(e) => setFullName(e.target.value)} disabled={create.isPending} />
            </div>
            {error ? (
              <p role="alert" className="text-xs text-destructive">
                {error}
              </p>
            ) : null}
            <div className="flex justify-end gap-2">
              <Dialog.Close disabled={create.isPending} render={<Button type="button" variant="outline" />}>
                Huỷ
              </Dialog.Close>
              <Button type="submit" disabled={create.isPending}>
                {create.isPending ? "Đang xử lý..." : "Thêm HR"}
              </Button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
