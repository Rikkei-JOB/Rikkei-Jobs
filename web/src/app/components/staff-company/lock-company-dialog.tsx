"use client"

import { ReasonDialog } from "@/app/components/staff-company/reason-dialog"
import { useLockCompany } from "@/hooks/use-staff-companies"
import { ApiError } from "@/services/api"

interface LockCompanyDialogProps {
  company: { id: string; name: string }
  onClose: () => void
  onDone: (message: string) => void
  /** 409 (công ty đã bị người khác khoá): hộp đóng, trang hiện message; chi tiết đã được làm mới. */
  onConflict: (message: string) => void
}

/** Hộp khoá công ty (DN-07): nêu hậu quả, lý do bắt buộc. Lỗi khác 409 (400 errors.reason, 503) hiện trong hộp. */
export function LockCompanyDialog({ company, onClose, onDone, onConflict }: LockCompanyDialogProps) {
  const lock = useLockCompany(company.id)

  async function confirm(reason: string) {
    try {
      await lock.mutateAsync(reason)
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 409) throw error
      onClose()
      onConflict(error.message)
      return
    }
    onClose()
    onDone("Đã khoá công ty")
  }

  return (
    <ReasonDialog
      title={`Khoá ${company.name}?`}
      description="Mọi HR của công ty sẽ không đăng nhập được, phiên đang mở bị đăng xuất. JD đang tuyển bị ẩn khỏi trang việc làm (không xoá), hiện lại khi mở khoá."
      confirmLabel="Khoá công ty"
      onConfirm={confirm}
      onClose={onClose}
    />
  )
}
