"use client"

import * as React from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { SelectMenu } from "@/app/components/ui/select-menu"
import { LEVEL_LABEL, STATUS_LABEL, formatDate, formatDateTime } from "@/app/components/job/job-labels"
import { JobStatusBadge } from "@/app/components/job/job-status-badge"
import { useStaffJobs } from "@/hooks/use-staff-jobs"
import type { StaffJobStatus } from "@/interface/staff-job"
import { getErrorMessage } from "@/services/api"

const PAGE_SIZE = 20

/** Cán bộ không thấy JD nháp, nên bộ lọc chỉ có Tất cả và 4 trạng thái của GET /staff/jobs. */
const STATUS_FILTER: StaffJobStatus[] = ["PENDING", "APPROVED", "REJECTED", "CLOSED"]

interface CompanyJobsTableProps {
  companyId: string
  /** undefined = Tất cả. */
  status: StaffJobStatus | undefined
  page: number
  onChange: (next: { status: StaffJobStatus | undefined; page: number }) => void
}

/** Tab "JD của công ty": lọc theo trạng thái, phân trang; bấm một dòng mở màn duyệt JD /staff/jobs/{id}. */
export function CompanyJobsTable({ companyId, status, page, onChange }: CompanyJobsTableProps) {
  const router = useRouter()
  const { data, error, isPending, isFetching, isPlaceholderData, refetch } = useStaffJobs({
    companyId,
    status,
    page,
    size: PAGE_SIZE,
  })
  const lastPage = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1

  // Trang hiện tại hết dữ liệu (JD vừa bị xoá hoặc đổi trạng thái): lùi về trang cuối còn dữ liệu.
  React.useEffect(() => {
    if (data && page > 1 && data.items.length === 0) onChange({ status, page: lastPage })
  }, [data, page, lastPage, status, onChange])

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-3">
        <SelectMenu
          value={status ?? ""}
          onValueChange={(value) => onChange({ status: (value || undefined) as StaffJobStatus | undefined, page: 1 })}
          aria-label="Lọc theo trạng thái JD"
          className="ui-select-trigger h-9 min-h-9 w-auto min-w-36 rounded-lg px-3 text-sm"
          options={[{ value: "", label: "Tất cả" }, ...STATUS_FILTER.map((s) => ({ value: s, label: STATUS_LABEL[s] }))]}
        />
        {data ? <span className="text-sm text-muted-foreground">{data.total} JD</span> : null}
      </div>

      {error ? (
        <Alert variant="destructive" className="flex flex-wrap items-center justify-between gap-2">
          <span>{getErrorMessage(error)}</span>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Thử lại
          </Button>
        </Alert>
      ) : isPending || !data ? (
        <p role="status" className="text-sm text-muted-foreground">Đang tải...</p>
      ) : data.items.length === 0 ? (
        <p className="rounded-xl border border-dashed bg-card px-6 py-10 text-center text-sm text-muted-foreground">
          {status ? `Không có JD nào ở trạng thái "${STATUS_LABEL[status]}".` : "Công ty chưa gửi JD nào lên hệ thống."}
        </p>
      ) : (
        <div className={isFetching && isPlaceholderData ? "opacity-60" : undefined}>
          <div className="overflow-x-auto rounded-xl border bg-card">
            <table className="w-full text-left text-sm">
              <thead className="bg-[#F8FAFC] text-xs text-muted-foreground">
                <tr>
                  <th className="px-4 py-3 font-semibold">Tiêu đề</th>
                  <th className="px-4 py-3 font-semibold">Trạng thái</th>
                  <th className="px-4 py-3 font-semibold whitespace-nowrap">Hạn nộp</th>
                  <th className="px-4 py-3 font-semibold whitespace-nowrap">Cập nhật</th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((job) => {
                  const href = `/staff/jobs/${encodeURIComponent(job.id)}`
                  return (
                    <tr key={job.id} className="cursor-pointer border-t align-top hover:bg-[#FFFAFA]" onClick={() => router.push(href)}>
                      <td className="max-w-sm px-4 py-3">
                        <Link
                          href={href}
                          onClick={(e) => e.stopPropagation()}
                          className="block truncate font-semibold text-heading hover:text-primary hover:underline"
                          title={job.title}
                        >
                          {job.title}
                        </Link>
                        <span className="mt-0.5 block text-xs text-muted-foreground">
                          {[job.level ? LEVEL_LABEL[job.level] : null, `${job.requiredSkillCount} kỹ năng bắt buộc`].filter(Boolean).join(" · ")}
                        </span>
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap">
                        <JobStatusBadge status={job.status} />
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap">{formatDate(job.deadline)}</td>
                      <td className="px-4 py-3 whitespace-nowrap text-muted-foreground">{formatDateTime(job.submittedAt, true)}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
            <span>
              Trang {data.page}/{lastPage} · {data.total} JD
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
