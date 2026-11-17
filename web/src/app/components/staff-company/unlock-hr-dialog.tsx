"use client"

import { Dialog } from "@base-ui/react/dialog"
import * as React from "react"
import { Button } from "@/app/components/ui/button"
import { useUnlockHr } from "@/hooks/use-staff-companies"
import type { StaffHrAccount } from "@/interface/company"
import { hrErrorMessage } from "./hr-error"

/** Hộp xác nhận mở khoá HR (không cần lý do). */
export function UnlockHrDialog({ account, onClose, onDone }: { account: StaffHrAccount; onClose: () => void; onDone: (message: string) => void }) {
  const unlock = useUnlockHr()
  const [error, setError] = React.useState("")

  function confirm() {
    if (unlock.isPending) return
    setError("")
    unlock.mutate(account.userId, {
      onSuccess: () => {
        onDone("Đã mở khoá tài khoản " + account.email)
        onClose()
      },
      onError: (err) => setError(hrErrorMessage(err)),
    })
  }

  return (
    <Dialog.Root
      open
      onOpenChange={(value) => {
        if (!value && !unlock.isPending) onClose()
      }}
    >
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/45" />
        <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-2xl border bg-background p-6 shadow-xl">
          <Dialog.Title className="text-base font-semibold">Mở khoá tài khoản {account.email}?</Dialog.Title>
          <Dialog.Description className="mb-4 mt-2 text-xs/relaxed text-muted-foreground">
            HR sẽ có thể đăng nhập lại bình thường.
          </Dialog.Description>
          {error ? (
            <p role="alert" className="mb-3 text-xs text-destructive">
              {error}
            </p>
          ) : null}
          <div className="flex justify-end gap-2">
            <Dialog.Close disabled={unlock.isPending} render={<Button type="button" variant="outline" />}>
              Huỷ
            </Dialog.Close>
            <Button type="button" onClick={confirm} disabled={unlock.isPending}>
              {unlock.isPending ? "Đang xử lý..." : "Mở khoá"}
            </Button>
          </div>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
