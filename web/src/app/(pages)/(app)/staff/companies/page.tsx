"use client"

import * as React from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { Input } from "@/app/components/ui/input"
import { CompanyTable } from "@/app/components/staff-company/company-table"
import { CreateCompanyDialog } from "@/app/components/staff-company/create-company-dialog"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useStaffCompanies } from "@/hooks/use-staff-companies"
import type { CompanyStatus } from "@/interface/company"
import { getErrorMessage } from "@/services/api"
import { SelectMenu } from "@/app/components/ui/select-menu"
import { TaskBanner } from "@/app/components/ui/task-banner"

const PAGE_SIZE = 20
const BASE_PATH = "/staff/companies"

function StaffCompaniesView() {
  const allowed = useAuthGuard({ roles: ["STAFF"] })
  const router = useRouter()
  const searchParams = useSearchParams()

  // q, status, page nằm trên URL; ô tìm có state riêng để debounce 300 ms rồi mới đẩy lên URL.
  const q = searchParams.get("q") ?? ""
  const rawStatus = searchParams.get("status")
  const status: CompanyStatus | undefined = rawStatus === "ACTIVE" || rawStatus === "LOCKED" ? rawStatus : undefined
  const rawPage = Number(searchParams.get("page") ?? "1")
  const page = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1

  const [search, setSearch] = React.useState(q)
  const [creating, setCreating] = React.useState(false)

  const navigate = React.useCallback(
    (next: { q: string; status?: CompanyStatus; page: number }) => {
      const query = new URLSearchParams()
      if (next.q) query.set("q", next.q)
      if (next.status) query.set("status", next.status)
      if (next.page > 1) query.set("page", String(next.page))
      router.replace(query.size ? `${BASE_PATH}?${query}` : BASE_PATH)
    },
    [router],
  )

  React.useEffect(() => {
    const trimmed = search.trim()
    if (trimmed === q) return
    const timer = setTimeout(() => navigate({ q: trimmed, status, page: 1 }), 300)
    return () => clearTimeout(timer)
  }, [search, q, status, navigate])

  const { data, error, isPending, isFetching, refetch } = useStaffCompanies({ q: q || undefined, status, page, size: PAGE_SIZE }, allowed)

  if (!allowed) return null

  const lastPage = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1

  return (
    <div className="space-y-4">
      <TaskBanner
        eyebrow="Quản lý đối tác"
        title="Doanh nghiệp"
        description="Tạo hồ sơ doanh nghiệp, cấp và quản lý tài khoản HR, khoá hoặc mở khoá khi cần."
        illustration="applications"
        compact
      >
        <Button data-primary onClick={() => setCreating(true)}>+ Tạo công ty</Button>
      </TaskBanner>

      {creating ? (
        <CreateCompanyDialog
          onClose={() => setCreating(false)}
          onCreated={(company) => {
            setCreating(false)
            router.push(`${BASE_PATH}/${company.id}`)
          }}
        />
      ) : null}

      <div className="flex flex-wrap items-center gap-2">
        <Input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Tìm theo tên hoặc mã số thuế"
          aria-label="Tìm công ty"
          className="max-w-xs"
        />
        <SelectMenu
          value={status ?? ""}
          onValueChange={(value) => navigate({ q, status: (value || undefined) as CompanyStatus | undefined, page: 1 })}
          aria-label="Lọc theo trạng thái"
          className="ui-select-trigger h-7 min-h-7 w-auto min-w-32 rounded-md px-2 text-xs"
          options={[{ value: "", label: "Tất cả" }, { value: "ACTIVE", label: "Hoạt động" }, { value: "LOCKED", label: "Đã khoá" }]}
        />
        {data ? <span className="text-xs text-muted-foreground">{data.total} công ty</span> : null}
      </div>

      {isPending && allowed ? <p className="text-xs text-muted-foreground">Đang tải...</p> : null}

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
          <CompanyTable items={data.items} />
          <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
            <span>
              Trang {data.page}/{lastPage}
            </span>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => navigate({ q, status, page: page - 1 })}>
                Trước
              </Button>
              <Button variant="outline" size="sm" disabled={page >= lastPage} onClick={() => navigate({ q, status, page: page + 1 })}>
                Sau
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

export default function StaffCompaniesPage() {
  return (
    <React.Suspense fallback={<p className="text-xs text-muted-foreground">Đang tải...</p>}>
      <StaffCompaniesView />
    </React.Suspense>
  )
}
