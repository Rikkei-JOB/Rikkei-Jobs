"use client"

import * as React from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/app/components/ui/tabs"
import { STATUS_LABEL } from "@/app/components/job/job-labels"
import { ReviewPanel } from "@/app/components/staff-job/review-panel"
import { ReviewQueue } from "@/app/components/staff-job/review-queue"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useStaffCompanies } from "@/hooks/use-staff-companies"
import { useStaffJobs } from "@/hooks/use-staff-jobs"
import { getErrorMessage } from "@/services/api"

const BASE_PATH = "/staff/jobs"
const PAGE_SIZE = 20
/** Các tab của màn duyệt; cán bộ không xem JD nháp. */
const TABS = ["PENDING", "APPROVED", "REJECTED", "CLOSED"] as const
type ReviewStatus = (typeof TABS)[number]

const EMPTY_TEXT: Record<ReviewStatus, string> = {
  PENDING: "Không có JD nào chờ duyệt",
  APPROVED: "Không có JD nào đang tuyển",
  REJECTED: "Không có JD nào bị trả lại",
  CLOSED: "Không có JD nào đã đóng",
}

interface ViewState {
  status: ReviewStatus
  companyId?: string
  page: number
  jobId?: string
}

function StaffJobsView() {
  const allowed = useAuthGuard({ roles: ["STAFF"] })
  const router = useRouter()
  const searchParams = useSearchParams()

  // status, companyId, page và jobId (JD đang chọn) nằm trên URL để mở thẳng từ trang công ty (Task 10).
  const rawStatus = searchParams.get("status")
  const status: ReviewStatus = TABS.find((s) => s === rawStatus) ?? "PENDING"
  const companyId = searchParams.get("companyId") || undefined
  const jobId = searchParams.get("jobId") || undefined
  const rawPage = Number(searchParams.get("page") ?? "1")
  const page = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1

  const [notice, setNotice] = React.useState("")
  const [conflict, setConflict] = React.useState("")
  // JD vừa xử lý: không tự chọn lại nó trong lúc hàng chờ còn dữ liệu cũ.
  const [doneId, setDoneId] = React.useState<string>()

  const navigate = React.useCallback(
    (next: ViewState) => {
      const query = new URLSearchParams()
      query.set("status", next.status)
      if (next.companyId) query.set("companyId", next.companyId)
      if (next.page > 1) query.set("page", String(next.page))
      if (next.jobId) query.set("jobId", next.jobId)
      router.replace(`${BASE_PATH}?${query}`, { scroll: false })
    },
    [router],
  )

  /** Đổi tab / công ty / trang: bỏ JD đang chọn và các thông báo cũ. */
  function changeView(next: Omit<ViewState, "jobId">) {
    setNotice("")
    setConflict("")
    setDoneId(undefined)
    navigate(next)
  }

  const companies = useStaffCompanies({ page: 1, size: 100 }, allowed)
  const queue = useStaffJobs({ status, companyId, page, size: PAGE_SIZE }, allowed)
  // Số trên tab "Chờ duyệt (n)" = total của truy vấn PENDING (cùng bộ lọc công ty).
  const pendingCount = useStaffJobs({ status: "PENDING", companyId, page: 1, size: 1 }, allowed)

  const items = React.useMemo(() => queue.data?.items ?? [], [queue.data])
  // Không mặc định chọn JD đầu tiên khi hàng chờ đang là dữ liệu cũ (của tab / trang / công ty trước).
  const selectedId = jobId ?? (queue.isPlaceholderData ? undefined : items.find((item) => item.id !== doneId)?.id)

  // Lần render mới nhất: ReviewPanel có thể báo xử lý xong sau khi cán bộ đã chọn JD khác (callback cũ).
  const latest = React.useRef({ selectedId, jobId, items, status, companyId, page })
  React.useEffect(() => {
    latest.current = { selectedId, jobId, items, status, companyId, page }
  })

  // Xử lý xong JD cuối cùng của một trang (> 1) thì lùi về trang trước.
  const emptyPage = !!queue.data && !queue.isPlaceholderData && queue.data.items.length === 0 && page > 1
  React.useEffect(() => {
    if (emptyPage) navigate({ status, companyId, page: page - 1 })
  }, [emptyPage, navigate, status, companyId, page])

  if (!allowed) return null

  const lastPage = queue.data ? Math.max(1, Math.ceil(queue.data.total / queue.data.size)) : 1
  const companyOptions = companies.data?.items ?? []
  const companyMissing = !!companyId && !companyOptions.some((c) => c.id === companyId)
  const missingName = companyMissing ? items.find((i) => i.company.id === companyId)?.company.name : undefined

  /**
   * Sau khi JD rời hàng chờ: chọn JD kế tiếp (hoặc liền trước nếu là JD cuối trang), chỉ khi JD vừa xử lý vẫn là
   * JD đang chọn; cán bộ đã chọn JD / tab khác trong lúc chờ thì giữ nguyên lựa chọn của họ.
   */
  function handleProcessed(processedId: string, message: string) {
    const current = latest.current
    setDoneId(processedId)
    setConflict("")
    setNotice(message)
    if (current.selectedId !== processedId) return
    const index = current.items.findIndex((item) => item.id === processedId)
    const next =
      index >= 0 ? (current.items[index + 1] ?? current.items[index - 1]) : current.items.find((item) => item.id !== processedId)
    navigate({ status: current.status, companyId: current.companyId, page: current.page, jobId: next?.id })
  }

  /** 409: giữ JD đang chọn (chi tiết đã tải lại, cán bộ thấy trạng thái mới), hàng chờ đã làm mới. */
  function handleConflict(processedId: string, message: string) {
    const current = latest.current
    setNotice("")
    setConflict(message)
    if (current.selectedId === processedId && processedId !== current.jobId) {
      navigate({ status: current.status, companyId: current.companyId, page: current.page, jobId: processedId })
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-semibold text-[#18233a]">Duyệt JD</h1>
        <label className="flex items-center gap-2 text-xs text-muted-foreground">
          Công ty
          <select
            value={companyId ?? ""}
            onChange={(e) => changeView({ status, companyId: e.target.value || undefined, page: 1 })}
            className="h-7 max-w-[16rem] rounded-md border border-input bg-background px-2 text-xs text-foreground"
          >
            <option value="">Tất cả</option>
            {companyMissing ? <option value={companyId}>{missingName ?? "Công ty đang chọn"}</option> : null}
            {companyOptions.map((company) => (
              <option key={company.id} value={company.id}>
                {company.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      {notice ? <Alert variant="success">{notice}</Alert> : null}
      {conflict ? <Alert variant="destructive">{conflict}</Alert> : null}

      <Tabs value={status} onValueChange={(value) => changeView({ status: value as ReviewStatus, companyId, page: 1 })}>
        <TabsList aria-label="Trạng thái JD" className="flex w-full overflow-x-auto sm:w-fit">
          {TABS.map((tab) => (
            <TabsTrigger key={tab} value={tab} className="shrink-0 px-3">
              {tab === "PENDING" && pendingCount.data ? `${STATUS_LABEL[tab]} (${pendingCount.data.total})` : STATUS_LABEL[tab]}
            </TabsTrigger>
          ))}
        </TabsList>

        <TabsContent value={status}>
          {queue.error ? (
            <Alert variant="destructive" className="flex items-center justify-between gap-2">
              <span>{getErrorMessage(queue.error)}</span>
              <Button variant="outline" size="sm" onClick={() => queue.refetch()}>
                Thử lại
              </Button>
            </Alert>
          ) : (
            <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
              <section aria-label="Hàng chờ" className="min-w-0">
                {queue.data ? (
                  <ReviewQueue
                    items={items}
                    selectedId={selectedId}
                    onSelect={(id) => {
                      setNotice("")
                      setConflict("")
                      navigate({ status, companyId, page, jobId: id })
                    }}
                    page={queue.data.page}
                    lastPage={lastPage}
                    total={queue.data.total}
                    onPage={(next) => changeView({ status, companyId, page: next })}
                    fetching={queue.isFetching}
                    emptyText={EMPTY_TEXT[status]}
                    timeLabel={status === "PENDING" ? "gửi" : "cập nhật"}
                  />
                ) : (
                  <p className="text-xs text-muted-foreground">Đang tải...</p>
                )}
              </section>
              <section aria-label="Chi tiết JD" className="min-w-0">
                {selectedId ? (
                  <ReviewPanel key={selectedId} jobId={selectedId} onProcessed={handleProcessed} onConflict={handleConflict} />
                ) : queue.data && !queue.isPlaceholderData ? (
                  <p className="rounded-xl border border-dashed bg-card py-10 text-center text-xs text-muted-foreground">
                    Chọn một JD để xem chi tiết
                  </p>
                ) : null}
              </section>
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  )
}

export default function StaffJobsPage() {
  return (
    <React.Suspense fallback={<p className="text-xs text-muted-foreground">Đang tải...</p>}>
      <StaffJobsView />
    </React.Suspense>
  )
}
