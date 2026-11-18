"use client"

import { Dialog } from "@base-ui/react/dialog"
import { Button } from "@/app/components/ui/button"

interface ConfirmDialogProps {
  title: string
  description: string
  confirmLabel: string
  onConfirm: () => void
  onCancel: () => void
  /** Đang xử lý: khoá cả hai nút và không cho đóng hộp. */
  pending?: boolean
  destructive?: boolean
}

/** Hộp xác nhận; chỉ render khi mở (theo mẫu add-hr-dialog). */
export function ConfirmDialog({ title, description, confirmLabel, onConfirm, onCancel, pending = false, destructive = false }: ConfirmDialogProps) {
  return (
    <Dialog.Root
      open
      onOpenChange={(value) => {
        if (!value && !pending) onCancel()
      }}
    >
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/45" />
        <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-2xl border bg-background p-6 shadow-xl">
          <Dialog.Title className="text-base font-semibold">{title}</Dialog.Title>
          <Dialog.Description className="mb-4 mt-2 text-xs/relaxed text-muted-foreground">{description}</Dialog.Description>
          <div className="flex justify-end gap-2">
            <Dialog.Close disabled={pending} render={<Button type="button" variant="outline" />}>
              Huỷ
            </Dialog.Close>
            <Button type="button" variant={destructive ? "destructive" : "default"} disabled={pending} onClick={onConfirm}>
              {pending ? "Đang xử lý..." : confirmLabel}
            </Button>
          </div>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
