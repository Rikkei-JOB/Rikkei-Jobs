"use client"

import * as React from "react"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { Tabs, TabsList, TabsTrigger } from "@/app/components/ui/tabs"
import { JobTable } from "@/app/components/job/job-table"
import { STATUS_LABEL, STATUS_ORDER } from "@/app/components/job/job-labels"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useMyCompany } from "@/hooks/use-my-company"
import { useMyJobs } from "@/hooks/use-my-jobs"
import type { JobStatus } from "@/interface/job"
import { getErrorMessage } from "@/services/api"

const BASE_PATH = "/hr/jobs"
const ALL = "ALL"

function HrJobsView() {
  const allowed = useAuthGuard({ roles: ["HR"] })
  const router = useRouter()
  const searchParams = useSearchParams()

  // status và page nằm trên URL (như staff/companies).
  const rawStatus = searchParams.get("status")
  const status = STATUS_ORDER.find((s) => s === rawStatus)
  const rawPage = Number(searchParams.get("page") ?? "1")
  const page = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1

  const navigate = React.useCallback(
    (next: { status?: JobStatus; page: number }) => {
      const query = new URLSearchParams()
      if (next.status) query.set("status", next.status)
      if (next.page > 1) query.set("page", String(next.page))
      router.replace(query.size ? `${BASE_PATH}?${query}` : BASE_PATH)
    },
    [router],
  )

  const { data: company } = useMyCompany(allowed)
  const { data, error, isPending, isFetching, refetch } = useMyJobs(status, page, allowed)

  if (!allowed) return null

  const locked = company?.status === "LOCKED"
  const lastPage = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1

  return (
    <div className="mx-auto max-w-5xl space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="font-heading text-lg font-semibold">JD của {company?.name ?? "công ty"}</h1>
        {locked ? null : (
          <Button size="sm" render={<Link href="/hr/jobs/new" />} nativeButton={false}>
            + Tạo JD
          </Button>
        )}
      </div>

      {locked ? <Alert>Công ty đang bị khoá, bạn chỉ xem được danh sách JD.</Alert> : null}

      <Tabs value={status ?? ALL} onValueChange={(value) => navigate({ status: STATUS_ORDER.find((s) => s === value), page: 1 })}>
        <TabsList className="grid-cols-3 sm:grid-cols-6">
          <TabsTrigger value={ALL}>Tất cả</TabsTrigger>
          {STATUS_ORDER.map((s) => (
            <TabsTrigger key={s} value={s}>
              {STATUS_LABEL[s]}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>

      {isPending ? <p className="text-xs text-muted-foreground">Đang tải...</p> : null}

      {error ? (
        <Alert variant="destructive" className="flex items-center justify-between gap-2">
          <span>{getErrorMessage(error)}</span>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Thử lại
          </Button>
        </Alert>
      ) : null}

      {data ? (
        <div className={isFetching ? "opacity-60" : undefined}>
          <JobTable items={data.items} />
          <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
            <span>
              {data.total} JD · Trang {data.page}/{lastPage}
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
