import { formatDateTime } from "@/app/components/job/job-labels"
import type { CompanyLockInfo, StaffActor } from "@/interface/company"

const CONSEQUENCE = "HR không đăng nhập được; JD đang tuyển bị ẩn khỏi trang việc làm."

/** Tên cán bộ; không tra được tên (identity lỗi) thì hiện id rút gọn. */
export function staffName(staff: StaffActor): string {
  return staff.fullName ?? `${staff.id.slice(0, 8)}…`
}

/** Lý do kết thúc bằng đúng một dấu chấm, để nối tiếp câu hậu quả. */
function withPeriod(text: string): string {
  return `${text.trim().replace(/[.\s]+$/, "")}.`
}

/** Banner đỏ khi công ty đang bị khoá; lock null (dữ liệu cũ chưa có dòng LOCK) thì chỉ nêu hậu quả. */
export function CompanyLockBanner({ lock }: { lock: CompanyLockInfo | null }) {
  return (
    <div role="status" className="rounded-lg border border-destructive/40 bg-destructive/10 px-3 py-2 text-xs/relaxed text-destructive">
      {lock ? (
        <>
          Công ty đang bị khoá từ {formatDateTime(lock.lockedAt, true)} bởi {staffName(lock.lockedBy)}.
          {lock.reason ? <span className="break-words whitespace-pre-line"> Lý do: {withPeriod(lock.reason)}</span> : null} {CONSEQUENCE}
        </>
      ) : (
        <>Công ty đang bị khoá. {CONSEQUENCE}</>
      )}
    </div>
  )
}
