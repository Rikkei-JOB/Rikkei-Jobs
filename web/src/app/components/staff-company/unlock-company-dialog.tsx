"use client"

import { Dialog } from "@base-ui/react/dialog"
import * as React from "react"
import { Button } from "@/app/components/ui/button"
import { useUnlockCompany } from "@/hooks/use-staff-companies"
import { ApiError, getErrorMessage } from "@/services/api"

interface UnlockCompanyDialogProps {
  company: { id: string; name: string }
  onClose: () => void
  onDone: (message: string) => void
  /** 409 (công ty đã được người khác mở khoá): hộp đóng, trang hiện message; chi tiết đã được làm mới. */
  onConflict: (message: string) => void
}

/** Hộp xác nhận mở khoá công ty (DN-07), không cần lý do. */
export function UnlockCompanyDialog({ company, onClose, onDone, onConflict }: UnlockCompanyDialogProps) {
  const unlock = useUnlockCompany(company.id)
  const [error, setError] = React.useState("")

  function confirm() {
    if (unlock.isPending) return
    setError("")
    unlock.mutate(undefined, {
      onSuccess: () => {
        onClose()
        onDone("Đã mở khoá công ty")
      },
      onError: (err) => {
        if (err instanceof ApiError && err.status === 409) {
          onClose()
          onConflict(err.message)
        } else {
          setError(getErrorMessage(err))
        }
      },
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
        <Dialog.Popup className="fixed top-1/2 left-1/2 z-50 w-[calc(100%-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-2xl border bg-background p-6 shadow-xl">
          <Dialog.Title className="text-xl font-semibold text-heading">Mở khoá {company.name}?</Dialog.Title>
          <Dialog.Description className="mt-2 text-sm/relaxed text-muted-foreground">
            HR của công ty đăng nhập lại được (trừ HR đang bị khoá riêng). JD đang tuyển còn hạn hiện lại trên trang việc làm.
          </Dialog.Description>
          {error ? (
            <p role="alert" className="mt-4 text-sm text-destructive">
              {error}
            </p>
          ) : null}
          <div className="mt-6 flex flex-wrap justify-end gap-3">
            <Dialog.Close disabled={unlock.isPending} render={<Button type="button" variant="outline" className="min-h-11" />}>
              Huỷ
            </Dialog.Close>
            <Button type="button" className="min-h-11" onClick={confirm} disabled={unlock.isPending}>
              {unlock.isPending ? "Đang xử lý..." : "Mở khoá"}
            </Button>
          </div>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
