"use client"

import Link from "next/link"
import { useRouter } from "next/navigation"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { formatDate, formatDateTime, STATUS_LABEL, STATUS_ORDER } from "@/app/components/job/job-labels"
import { JobStatusBadge } from "@/app/components/job/job-status-badge"
import { useStaffJobs } from "@/hooks/use-staff-jobs"
import type { StaffJobItem, StaffJobStatus } from "@/interface/staff-job"
import { getErrorMessage } from "@/services/api"

const PAGE_SIZE = 20

interface CompanyJobsTableProps {
  companyId: string
  /** undefined = Tất cả. */
  status: StaffJobStatus | undefined
  page: number
  onChange: (next: { status: StaffJobStatus | undefined; page: number }) => void
}

/** Mở JD ở màn duyệt JD của cán bộ (UI-01: /staff/jobs/{id}). Cán bộ không thấy JD nháp nên mọi dòng đều mở được. */
function reviewHref(job: StaffJobItem): string | null {
  return `/staff/jobs/${encodeURIComponent(job.id)}`
}

/** Tab "JD của công ty": Tiêu đề / Trạng thái / Hạn nộp / Cập nhật, lọc theo trạng thái, có phân trang. */
export function CompanyJobsTable({ companyId, status, page, onChange }: CompanyJobsTableProps) {
  const router = useRouter()
  const { data, error, isPending, isFetching, isPlaceholderData, refetch } = useStaffJobs({
    companyId,
    status,
    page,
    size: PAGE_SIZE,
  })
  const lastPage = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <select
          value={status ?? ""}
          onChange={(e) => onChange({ status: (e.target.value || undefined) as StaffJobStatus | undefined, page: 1 })}
          aria-label="Lọc theo trạng thái JD"
          className="h-7 rounded-md border border-input bg-background px-2 text-xs"
        >
          <option value="">Tất cả</option>
          {STATUS_ORDER.filter((s): s is StaffJobStatus => s !== "DRAFT").map((s) => (
            <option key={s} value={s}>
              {STATUS_LABEL[s]}
            </option>
          ))}
        </select>
        {data ? <span className="text-xs text-muted-foreground">{data.total} JD</span> : null}
      </div>

      {error ? (
        <Alert variant="destructive" className="flex items-center justify-between gap-2">
          <span>{getErrorMessage(error)}</span>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Thử lại
          </Button>
        </Alert>
      ) : isPending || !data ? (
        <p className="text-xs text-muted-foreground">Đang tải...</p>
      ) : data.items.length === 0 ? (
        <p className="rounded-md border border-dashed bg-white py-8 text-center text-xs text-muted-foreground">
          {status ? `Không có JD nào ở trạng thái "${STATUS_LABEL[status]}".` : "Công ty chưa có JD nào."}
        </p>
      ) : (
        <div className={isFetching && isPlaceholderData ? "opacity-60" : undefined}>
          <div className="overflow-x-auto rounded-xl border bg-white">
            <table className="w-full text-left text-xs">
              <thead className="bg-muted/50 text-muted-foreground">
                <tr>
                  <th className="px-3 py-2 font-medium">Tiêu đề</th>
                  <th className="px-3 py-2 font-medium">Trạng thái</th>
                  <th className="px-3 py-2 font-medium whitespace-nowrap">Hạn nộp</th>
                  <th className="px-3 py-2 font-medium whitespace-nowrap">Cập nhật</th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((job) => {
                  const href = reviewHref(job)
                  return (
                    <tr
                      key={job.id}
                      className={href ? "cursor-pointer border-t hover:bg-muted/40" : "border-t"}
                      onClick={href ? () => router.push(href) : undefined}
                    >
                      <td className="max-w-sm px-3 py-2">
                        {href ? (
                          <Link
                            href={href}
                            onClick={(e) => e.stopPropagation()}
                            className="block truncate font-bold hover:underline"
                            title={job.title}
                          >
                            {job.title}
                          </Link>
                        ) : (
                          <span className="block truncate font-bold" title={`${job.title} (JD nháp, HR chưa gửi duyệt)`}>
                            {job.title}
                          </span>
                        )}
                      </td>
                      <td className="px-3 py-2 whitespace-nowrap">
                        <JobStatusBadge status={job.status} />
                      </td>
                      <td className="px-3 py-2 whitespace-nowrap">{formatDate(job.deadline)}</td>
                      <td className="px-3 py-2 whitespace-nowrap">{formatDateTime(job.submittedAt, true)}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
            <span>
              Trang {data.page}/{lastPage}
            </span>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => onChange({ status, page: page - 1 })}>
                Trước
              </Button>
              <Button variant="outline" size="sm" disabled={page >= lastPage} onClick={() => onChange({ status, page: page + 1 })}>
                Sau
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
