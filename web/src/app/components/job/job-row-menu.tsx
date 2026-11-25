"use client"

import * as React from "react"
import Link from "next/link"
import { Menu } from "@base-ui/react/menu"
import { MoreHorizontal } from "lucide-react"
import { ConfirmDialog } from "@/app/components/job/confirm-dialog"
import { useCloseJob, useSubmitJob } from "@/hooks/use-my-jobs"
import type { JobSummary } from "@/interface/job"
import { ApiError, getErrorMessage } from "@/services/api"

/** Kết quả thao tác từ menu; trang danh sách hiển thị ở đầu trang. */
export interface JobRowFeedback {
  variant: "success" | "destructive"
  message: string
  /** Có thì hiện liên kết "Mở để sửa" tới JD. */
  href?: string
}

interface JobRowMenuProps {
  job: JobSummary
  companyLocked: boolean
  /** null = xoá thông báo cũ khi bắt đầu thao tác mới. */
  onFeedback?: (feedback: JobRowFeedback | null) => void
}

const itemClass = "flex cursor-pointer items-center rounded-md px-2 py-1.5 text-xs outline-none data-[highlighted]:bg-muted"

/** Menu "⋯" của một dòng JD: Sửa / Gửi duyệt / Đóng JD / Xem tuỳ trạng thái. */
export function JobRowMenu({ job, companyLocked, onFeedback }: JobRowMenuProps) {
  const [confirmClose, setConfirmClose] = React.useState(false)
  const submit = useSubmitJob()
  const close = useCloseJob()
  const href = `/hr/jobs/${job.id}`

  const canSubmit = !companyLocked && (job.status === "DRAFT" || job.status === "REJECTED")
  const canClose = !companyLocked && job.status === "APPROVED"
  const canEdit = canSubmit || canClose

  // mutateAsync (không dùng callback của mutate): hàng có thể bị gỡ khỏi danh sách ngay khi cache làm mới,
  // khi đó callback theo từng lần gọi của TanStack Query bị bỏ qua.
  async function handleSubmit() {
    onFeedback?.(null)
    try {
      await submit.mutateAsync(job.id)
      onFeedback?.({ variant: "success", message: "Đã gửi duyệt" })
    } catch (error) {
      onFeedback?.(
        error instanceof ApiError && error.code === "VALIDATION_ERROR"
          ? { variant: "destructive", message: "JD chưa đủ điều kiện gửi duyệt", href }
          : { variant: "destructive", message: getErrorMessage(error) },
      )
    }
  }

  async function handleClose() {
    onFeedback?.(null)
    try {
      await close.mutateAsync(job.id)
      onFeedback?.({ variant: "success", message: "Đã đóng JD" })
    } catch (error) {
      onFeedback?.({ variant: "destructive", message: getErrorMessage(error) })
    } finally {
      setConfirmClose(false)
    }
  }

  return (
    // Chặn nổi bọt (kể cả sự kiện từ hộp thoại portal) để không mở dòng.
    <div onClick={(e) => e.stopPropagation()} className="flex justify-end">
      <Menu.Root>
        <Menu.Trigger
          aria-label="Thao tác"
          className="inline-flex size-7 items-center justify-center rounded-md text-muted-foreground hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <MoreHorizontal className="size-4" aria-hidden="true" />
        </Menu.Trigger>
        <Menu.Portal>
          <Menu.Positioner side="bottom" align="end" sideOffset={4} className="z-50">
            <Menu.Popup className="min-w-32 rounded-lg border bg-background p-1 shadow-md">
              <Menu.LinkItem render={<Link href={href} />} closeOnClick className={itemClass}>
                {canEdit ? "Sửa" : "Xem"}
              </Menu.LinkItem>
              {canSubmit ? (
                <Menu.Item disabled={submit.isPending} onClick={() => void handleSubmit()} className={itemClass}>
                  Gửi duyệt
                </Menu.Item>
              ) : null}
              {canClose ? (
                <Menu.Item onClick={() => setConfirmClose(true)} className={`${itemClass} text-destructive`}>
                  Đóng JD
                </Menu.Item>
              ) : null}
            </Menu.Popup>
          </Menu.Positioner>
        </Menu.Portal>
      </Menu.Root>
      {confirmClose ? (
        <ConfirmDialog
          title="Đóng JD?"
          description="Ứng viên sẽ không còn thấy JD này và không nộp hồ sơ vào được nữa. JD đã đóng không mở lại được."
          confirmLabel="Đóng JD"
          destructive
          pending={close.isPending}
          onConfirm={() => void handleClose()}
          onCancel={() => setConfirmClose(false)}
        />
      ) : null}
    </div>
  )
}
