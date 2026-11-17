"use client"

import { Dialog } from "@base-ui/react/dialog"
import * as React from "react"
import { Button } from "@/app/components/ui/button"
import { useLockHr } from "@/hooks/use-staff-companies"
import type { StaffHrAccount } from "@/interface/company"
import { lockReasonSchema } from "@/schemas/company.schema"
import { hrErrorMessage } from "./hr-error"

/** Hộp xác nhận khoá HR; chỉ render khi có HR được chọn (đóng = unmount nên form tự reset). */
export function LockHrDialog({ account, onClose, onDone }: { account: StaffHrAccount; onClose: () => void; onDone: (message: string) => void }) {
  const lock = useLockHr()
  const [reason, setReason] = React.useState("")
  const [error, setError] = React.useState("")

  function submit(event: React.FormEvent) {
    event.preventDefault()
    if (lock.isPending) return
    const parsed = lockReasonSchema.safeParse(reason)
    if (!parsed.success) {
      setError(parsed.error.issues[0].message)
      return
    }
    setError("")
    lock.mutate(
      { userId: account.userId, reason: parsed.data },
      {
        onSuccess: () => {
          onDone("Đã khoá tài khoản " + account.email)
          onClose()
        },
        onError: (err) => setError(hrErrorMessage(err)),
      },
    )
  }

  return (
    <Dialog.Root
      open
      onOpenChange={(value) => {
        if (!value && !lock.isPending) onClose()
      }}
    >
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/45" />
        <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-2xl border bg-background p-6 shadow-xl">
          <Dialog.Title className="text-base font-semibold">Khoá tài khoản {account.email}?</Dialog.Title>
          <Dialog.Description className="mb-4 mt-2 text-xs/relaxed text-muted-foreground">
            HR sẽ bị đăng xuất ngay ở thao tác kế tiếp và không đăng nhập được cho tới khi được mở khoá.
          </Dialog.Description>
          <form onSubmit={submit} noValidate className="space-y-3">
            <label htmlFor="lock-reason" className="block text-xs font-medium">
              Lý do khoá <span className="text-destructive">*</span>
            </label>
            <textarea
              id="lock-reason"
              rows={4}
              maxLength={500}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              disabled={lock.isPending}
              className="w-full rounded-md border border-input bg-background p-2 text-xs"
            />
            {error ? (
              <p role="alert" className="text-xs text-destructive">
                {error}
              </p>
            ) : null}
            <div className="flex justify-end gap-2">
              <Dialog.Close disabled={lock.isPending} render={<Button type="button" variant="outline" />}>
                Huỷ
              </Dialog.Close>
              <Button type="submit" variant="destructive" disabled={lock.isPending}>
                {lock.isPending ? "Đang xử lý..." : "Khoá tài khoản"}
              </Button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
