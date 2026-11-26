"use client"

import * as React from "react"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { daysUntil } from "@/app/components/job/job-deadline"
import { formatDateTime } from "@/app/components/job/job-labels"
import { JobPublicView } from "@/app/components/job/job-public-view"
import { JobStatusBadge } from "@/app/components/job/job-status-badge"
import { ReasonDialog } from "@/app/components/staff-job/reason-dialog"
import { useApproveStaffJob, useCloseStaffJob, useRejectStaffJob, useStaffJob } from "@/hooks/use-staff-jobs"
import type { StaffJobDetail } from "@/interface/staff-job"
import { ApiError, getErrorMessage } from "@/services/api"

interface ReviewPanelProps {
  jobId: string
  /**
   * Duyệt / trả lại / đóng xong JD processedId; message là thông báo cho cán bộ. Có thể được gọi sau khi khối này
   * đã unmount (cán bộ chọn JD khác lúc đang chờ), nên bên gọi so processedId với JD đang chọn.
   */
  onProcessed: (processedId: string, message: string) => void
  /** 409 (INVALID_JOB_STATUS, JOB_EXPIRED, CONFLICT) của JD processedId: message của backend; hàng chờ đã được làm mới. */
  onConflict: (processedId: string, message: string) => void
}

function isConflict(error: unknown): error is ApiError {
  return error instanceof ApiError && error.status === 409
}

/** JD chờ duyệt mà hạn nộp đã qua (giờ VN): backend chỉ trả 409 JOB_EXPIRED nên không cho bấm "Duyệt". */
function isExpiredPending(detail: StaffJobDetail): boolean {
  return detail.status === "PENDING" && detail.job.deadline !== null && daysUntil(detail.job.deadline) < 0
}

/** Dải trên cùng: trạng thái, nhãn công ty bị khoá, lý do và người / thời điểm xử lý gần nhất. */
function ReviewNotice({ detail }: { detail: StaffJobDetail }) {
  const expired = isExpiredPending(detail)
  const showReview = detail.status === "REJECTED" || detail.status === "CLOSED"
  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-2">
        <JobStatusBadge status={detail.status} />
        {detail.companyLocked ? (
          <span className="rounded-full bg-destructive/10 px-2 py-0.5 text-xs font-medium text-destructive">Công ty đang bị khoá</span>
        ) : null}
      </div>
      {expired ? <p className="text-xs text-destructive">Hạn nộp đã qua, JD này không duyệt được.</p> : null}
      {showReview && (detail.rejectReason || detail.reviewedAt) ? (
        <div className="space-y-1 rounded-lg bg-muted/60 p-3 text-xs">
          {detail.rejectReason ? (
            <p className="break-words whitespace-pre-line text-foreground">
              <span className="font-medium">Lý do trả lại:</span> {detail.rejectReason}
            </p>
          ) : null}
          {detail.reviewedAt ? (
            <p className="text-muted-foreground">
              Cán bộ xử lý gần nhất: {detail.reviewedBy?.fullName ?? "Không rõ tên"} · {formatDateTime(detail.reviewedAt, true)}
            </p>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}

/** Chi tiết JD đang chọn (như ứng viên thấy) cùng thanh nút Duyệt / Trả lại… / Đóng JD… theo trạng thái. */
export function ReviewPanel({ jobId, onProcessed, onConflict }: ReviewPanelProps) {
  const { data: detail, error, isPending, refetch } = useStaffJob(jobId)
  const approve = useApproveStaffJob()
  const reject = useRejectStaffJob()
  const close = useCloseStaffJob()
  const [dialog, setDialog] = React.useState<"reject" | "close" | null>(null)
  const [actionError, setActionError] = React.useState("")
  const busy = approve.isPending || reject.isPending || close.isPending
  // Chặn bấm "Duyệt" hai lần trước khi isPending kịp render lại.
  const approving = React.useRef(false)

  if (isPending) return <p className="rounded-xl border bg-card p-5 text-xs text-muted-foreground">Đang tải JD...</p>

  if (error || !detail) {
    return (
      <Alert variant="destructive" className="flex items-center justify-between gap-2">
        <span>{getErrorMessage(error)}</span>
        <Button variant="outline" size="sm" onClick={() => refetch()}>
          Thử lại
        </Button>
      </Alert>
    )
  }

  const title = detail.job.title

  // mutateAsync (không dùng callback của mutate): callback bị bỏ nếu khối này đã unmount khi request xong.
  async function runApprove() {
    if (busy || approving.current) return
    approving.current = true
    setActionError("")
    try {
      await approve.mutateAsync(jobId)
    } catch (err) {
      if (isConflict(err)) onConflict(jobId, err.message)
      else setActionError(getErrorMessage(err))
      return
    } finally {
      approving.current = false
    }
    onProcessed(jobId, "Đã duyệt JD")
  }

  /** Lỗi 409 đóng hộp và báo lên trang; lỗi khác ném lại để hộp hiện (errors.reason dưới ô). */
  async function runWithReason(action: "reject" | "close", reason: string) {
    try {
      if (action === "reject") await reject.mutateAsync({ id: jobId, reason })
      else await close.mutateAsync({ id: jobId, reason })
    } catch (err) {
      if (!isConflict(err)) throw err
      setDialog(null)
      onConflict(jobId, err.message)
      return
    }
    setDialog(null)
    onProcessed(jobId, action === "reject" ? "Đã trả lại JD" : "Đã đóng JD")
  }

  return (
    <>
      <JobPublicView job={detail.job} notice={<ReviewNotice detail={detail} />}>
        {actionError ? (
          <Alert variant="destructive" className="mb-3">
            {actionError}
          </Alert>
        ) : null}
        {detail.status === "PENDING" || detail.status === "APPROVED" ? (
          <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
            {detail.status === "PENDING" ? (
              <>
                <Button
                  variant="outline"
                  size="lg"
                  disabled={busy}
                  onClick={() => {
                    setActionError("")
                    setDialog("reject")
                  }}
                >
                  Trả lại…
                </Button>
                <Button size="lg" disabled={busy || isExpiredPending(detail)} onClick={() => void runApprove()}>
                  {approve.isPending ? "Đang duyệt..." : "Duyệt"}
                </Button>
              </>
            ) : (
              <Button
                variant="destructive"
                size="lg"
                disabled={busy}
                onClick={() => {
                  setActionError("")
                  setDialog("close")
                }}
              >
                Đóng JD…
              </Button>
            )}
          </div>
        ) : null}
      </JobPublicView>

      {dialog === "reject" ? (
        <ReasonDialog
          title={`Trả lại JD “${title}”?`}
          description="HR sẽ thấy lý do này để sửa JD và gửi duyệt lại."
          confirmLabel="Trả lại JD"
          onConfirm={(reason) => runWithReason("reject", reason)}
          onClose={() => setDialog(null)}
        />
      ) : null}
      {dialog === "close" ? (
        <ReasonDialog
          title={`Đóng JD “${title}”?`}
          description="JD sẽ ngừng hiển thị với ứng viên. Lý do được lưu trong lịch sử của công ty."
          confirmLabel="Đóng JD"
          onConfirm={(reason) => runWithReason("close", reason)}
          onClose={() => setDialog(null)}
        />
      ) : null}
    </>
  )
}
