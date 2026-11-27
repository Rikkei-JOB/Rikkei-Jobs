"use client"

import * as React from "react"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { Plus, UserSearch } from "lucide-react"
import { Alert } from "@/app/components/ui/alert"
import { TaskBanner } from "@/app/components/ui/task-banner"
import { Button } from "@/app/components/ui/button"
import { JobTable } from "@/app/components/job/job-table"
import { clearJobListNotice, peekJobListNotice } from "@/app/components/job/job-flash"
import { JobStats } from "@/app/components/job/job-stats"
import type { JobRowFeedback } from "@/app/components/job/job-row-menu"
import { STATUS_ORDER } from "@/app/components/job/job-labels"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useMyCompany } from "@/hooks/use-my-company"
import { useMyJobSummary, useMyJobs } from "@/hooks/use-my-jobs"
import type { JobStatus } from "@/interface/job"
import { getErrorMessage } from "@/services/api"

const BASE_PATH = "/hr/jobs"

function HrJobsView() {
  const allowed = useAuthGuard({ roles: ["HR"] })
  const router = useRouter()
  const searchParams = useSearchParams()

  // status và page nằm trên URL (như staff/companies).
  const rawStatus = searchParams.get("status")
  const status = STATUS_ORDER.find((s) => s === rawStatus)
  const rawPage = Number(searchParams.get("page") ?? "1")
  const page = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1

  const [feedback, setFeedback] = React.useState<JobRowFeedback | null>(null)

  const navigate = React.useCallback(
    (next: { status?: JobStatus; page: number }) => {
      setFeedback(null)
      const query = new URLSearchParams()
      if (next.status) query.set("status", next.status)
      if (next.page > 1) query.set("page", String(next.page))
      router.replace(query.size ? `${BASE_PATH}?${query}` : BASE_PATH)
    },
    [router],
  )

  // Thông báo từ form JD ("Đã gửi duyệt"...), đọc một lần khi vào trang.
  const [notice] = React.useState(peekJobListNotice)
  React.useEffect(() => clearJobListNotice(), [])

  const { data: company } = useMyCompany(allowed)
  const { data: summary } = useMyJobSummary(allowed)
  const { data, error, isPending, isFetching, refetch } = useMyJobs(status, page, allowed)

  if (!allowed) return null

  const locked = company?.status === "LOCKED"
  const lastPage = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1

  return (
    <div className="hr-page">
      <TaskBanner
        eyebrow="Quản lý tuyển dụng"
        title="Tin tuyển dụng"
        description={`Quản lý vị trí đang tuyển và tiến trình duyệt tin của ${company?.name ?? "công ty"}.`}
        illustration="jobs"
      >
        {locked ? null : <Link href="/hr/jobs/new" data-primary=""><Plus className="size-4" aria-hidden="true" />Tạo tin tuyển dụng</Link>}
        <Link href="/hr/recruitment"><UserSearch className="size-4" aria-hidden="true" />Hồ sơ ứng tuyển</Link>
      </TaskBanner>

      {notice ? <Alert variant="success">{notice}</Alert> : null}
      {locked ? <Alert>Công ty đang bị khoá, bạn chỉ xem được danh sách tin tuyển dụng.</Alert> : null}

      {feedback ? (
        <Alert variant={feedback.variant} className="flex flex-wrap items-center justify-between gap-2">
          <span>{feedback.message}</span>
          <span className="flex items-center gap-3">
          {feedback.href ? (
            <Link href={feedback.href} className="font-medium underline">
              Mở để sửa
            </Link>
          ) : null}
            <button type="button" aria-label="Đóng thông báo" onClick={() => setFeedback(null)} className="text-base leading-none focus-visible:outline-2 focus-visible:outline-primary">
              ×
            </button>
          </span>
        </Alert>
      ) : null}

      <JobStats summary={summary} active={status} onSelect={(next) => navigate({ status: next, page: 1 })} />

      {isPending ? <p className="text-xs text-muted-foreground">Đang tải...</p> : null}

      {error ? (
        <Alert variant="destructive" className="flex items-center justify-between gap-2">
          <span>{getErrorMessage(error)}</span>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Thử lại
          </Button>
        </Alert>
      ) : null}

      {data && !error ? (
        <div className={isFetching ? "opacity-60" : undefined}>
          <JobTable items={data.items} companyLocked={locked} onFeedback={setFeedback} />
          <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
            <span>
              {data.total} tin tuyển dụng · Trang {data.page}/{lastPage}
            </span>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => navigate({ status, page: page - 1 })}>
                Trước
              </Button>
              <Button variant="outline" size="sm" disabled={page >= lastPage} onClick={() => navigate({ status, page: page + 1 })}>
                Sau
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

export default function HrJobsPage() {
  return (
    <React.Suspense fallback={<p className="text-xs text-muted-foreground">Đang tải...</p>}>
      <HrJobsView />
    </React.Suspense>
  )
}
