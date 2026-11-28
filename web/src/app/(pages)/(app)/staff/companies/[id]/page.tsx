"use client"

import * as React from "react"
import Link from "next/link"
import { useParams, useRouter, useSearchParams } from "next/navigation"
import { ArrowLeft, Lock, LockOpen, UserPlus } from "lucide-react"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/app/components/ui/tabs"
import { TaskBanner } from "@/app/components/ui/task-banner"
import { CompanyMark } from "@/app/components/company/company-mark"
import { formatDate } from "@/app/components/job/job-labels"
import { AddHrDialog } from "@/app/components/staff-company/add-hr-dialog"
import { CompanyHistoryTable } from "@/app/components/staff-company/company-history-table"
import { CompanyJobsTable } from "@/app/components/staff-company/company-jobs-table"
import { CompanyLockBanner } from "@/app/components/staff-company/company-lock-banner"
import { HrAccountTable } from "@/app/components/staff-company/hr-account-table"
import { hrErrorMessage } from "@/app/components/staff-company/hr-error"
import { LockCompanyDialog } from "@/app/components/staff-company/lock-company-dialog"
import { LockHrDialog } from "@/app/components/staff-company/lock-hr-dialog"
import { UnlockCompanyDialog } from "@/app/components/staff-company/unlock-company-dialog"
import { UnlockHrDialog } from "@/app/components/staff-company/unlock-hr-dialog"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useStaffCompany } from "@/hooks/use-staff-companies"
import type { StaffCompanyDetail, StaffHrAccount } from "@/interface/company"
import type { StaffJobStatus } from "@/interface/staff-job"

/** Các tab của trang; tab đang chọn nằm trên URL (?tab=hr|jobs|history), mặc định hr. */
const TABS = ["hr", "jobs", "history"] as const
type DetailTab = (typeof TABS)[number]

/** Dòng mô tả trong banner: mã số thuế rồi ngành, quy mô, thành phố nếu có. */
function bannerDescription(company: StaffCompanyDetail): string {
  return [`MST ${company.taxCode}`, company.industry, company.size ? `${company.size} nhân sự` : null, company.city]
    .filter(Boolean)
    .join(" · ")
}

function StaffCompanyDetailView() {
  const allowed = useAuthGuard({ roles: ["STAFF"] })
  const { id } = useParams<{ id: string }>()
  const router = useRouter()
  const searchParams = useSearchParams()
  const rawTab = searchParams.get("tab")
  const tab: DetailTab = TABS.find((t) => t === rawTab) ?? "hr"
  const { data: company, error, isPending, refetch } = useStaffCompany(id, allowed)

  const [lockTarget, setLockTarget] = React.useState<StaffHrAccount | null>(null)
  const [unlockTarget, setUnlockTarget] = React.useState<StaffHrAccount | null>(null)
  const [adding, setAdding] = React.useState(false)
  const [companyDialog, setCompanyDialog] = React.useState<"lock" | "unlock" | null>(null)
  const [notice, setNotice] = React.useState("")
  const [conflict, setConflict] = React.useState("")
  // Bộ lọc / trang của hai tab dưới giữ nguyên khi chuyển qua lại giữa các tab.
  const [jobsView, setJobsView] = React.useState<{ status: StaffJobStatus | undefined; page: number }>({ status: undefined, page: 1 })
  const [historyPage, setHistoryPage] = React.useState(1)

  if (!allowed) return null

  function selectTab(next: DetailTab) {
    router.replace(`/staff/companies/${id}?tab=${next}`, { scroll: false })
  }

  function clearMessages() {
    setNotice("")
    setConflict("")
  }

  const locked = company?.status === "LOCKED"
  const tabs: { id: DetailTab; label: string }[] = [
    { id: "hr", label: company ? `Tài khoản HR (${company.hrAccounts.length})` : "Tài khoản HR" },
    { id: "jobs", label: "JD của công ty" },
    { id: "history", label: "Lịch sử thao tác" },
  ]

  return (
    <div className="w-full space-y-5">
      <Link
        href="/staff/companies"
        className="inline-flex min-h-9 items-center gap-1.5 text-sm font-semibold text-muted-foreground hover:text-primary"
      >
        <ArrowLeft className="size-4" aria-hidden="true" />
        Doanh nghiệp
      </Link>

      <TaskBanner
        compact
        eyebrow="Quản lý đối tác"
        illustration="company"
        title={company?.name ?? "Chi tiết doanh nghiệp"}
        description={company ? bannerDescription(company) : "Tài khoản HR, JD đã gửi lên và lịch sử thao tác của doanh nghiệp."}
      >
        {company ? (
          <>
            <span
              className={`rounded-full border px-3 py-1 text-xs font-semibold ${
                locked ? "border-white bg-white text-[#B4232D]" : "border-white/50 bg-white/15 text-white"
              }`}
            >
              {locked ? "Đã khoá" : "Đang hoạt động"}
            </span>
            <Button data-primary onClick={() => setAdding(true)}>
              <UserPlus aria-hidden="true" />
              Thêm HR
            </Button>
            {locked ? (
              <Button
                onClick={() => {
                  clearMessages()
                  setCompanyDialog("unlock")
                }}
              >
                <LockOpen aria-hidden="true" />
                Mở khoá công ty
              </Button>
            ) : (
              <Button
                onClick={() => {
                  clearMessages()
                  setCompanyDialog("lock")
                }}
              >
                <Lock aria-hidden="true" />
                Khoá công ty
              </Button>
            )}
          </>
        ) : null}
      </TaskBanner>

      {isPending ? <p role="status" className="text-sm text-muted-foreground">Đang tải...</p> : null}

      {error ? (
        <Alert variant="destructive" className="flex flex-wrap items-center justify-between gap-2">
          <span>{hrErrorMessage(error)}</span>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Thử lại
          </Button>
        </Alert>
      ) : null}

      {notice ? <Alert variant="success">{notice}</Alert> : null}
      {conflict ? <Alert variant="destructive">{conflict}</Alert> : null}

      {company ? (
        <>
          {locked ? <CompanyLockBanner lock={company.lock} /> : null}

          <div className="grid items-start gap-5 min-[1200px]:grid-cols-[minmax(0,1fr)_280px]">
            <Tabs className="min-w-0" value={tab} onValueChange={(value) => selectTab(value as DetailTab)}>
              <TabsList aria-label="Chi tiết doanh nghiệp" className="grid-cols-3">
                {tabs.map((t) => (
                  <TabsTrigger key={t.id} value={t.id} className="whitespace-normal">
                    {t.label}
                  </TabsTrigger>
                ))}
              </TabsList>
              <TabsContent value="hr">
                <HrAccountTable
                  accounts={company.hrAccounts}
                  onLock={(account) => {
                    clearMessages()
                    setLockTarget(account)
                  }}
                  onUnlock={(account) => {
                    clearMessages()
                    setUnlockTarget(account)
                  }}
                />
              </TabsContent>
              <TabsContent value="jobs">
                <CompanyJobsTable companyId={company.id} status={jobsView.status} page={jobsView.page} onChange={setJobsView} />
              </TabsContent>
              <TabsContent value="history">
                <CompanyHistoryTable companyId={company.id} page={historyPage} onPage={setHistoryPage} />
              </TabsContent>
            </Tabs>

            <aside className="rounded-xl border bg-card p-5" aria-label="Thông tin doanh nghiệp">
              <div className="flex items-center gap-3">
                <CompanyMark name={company.name} logoUrl={company.logoUrl} size={44} brand />
                <div className="min-w-0">
                  <p className="font-semibold break-words text-heading">{company.name}</p>
                  <p className="text-xs text-muted-foreground">Tạo ngày {formatDate(company.createdAt)}</p>
                </div>
              </div>
              <dl className="mt-4 space-y-3 border-t pt-4 text-sm">
                <CompanyFact label="Mã số thuế" value={company.taxCode} />
                <CompanyFact label="Ngành" value={company.industry} />
                <CompanyFact label="Quy mô" value={company.size ? `${company.size} nhân sự` : null} />
                <CompanyFact label="Địa chỉ" value={[company.address, company.city].filter(Boolean).join(", ") || null} />
                <div>
                  <dt className="text-xs text-muted-foreground">Website</dt>
                  <dd className="break-all">
                    {company.website ? (
                      <a href={company.website} target="_blank" rel="noreferrer noopener" className="font-medium text-primary hover:underline">
                        {company.website}
                      </a>
                    ) : (
                      <span className="text-muted-foreground">Chưa có</span>
                    )}
                  </dd>
                </div>
                <CompanyFact label="Tài khoản HR" value={`${company.hrAccounts.length} tài khoản`} />
              </dl>
            </aside>
          </div>

          {lockTarget ? <LockHrDialog key={lockTarget.userId} account={lockTarget} onClose={() => setLockTarget(null)} onDone={setNotice} /> : null}
          {unlockTarget ? (
            <UnlockHrDialog key={unlockTarget.userId} account={unlockTarget} onClose={() => setUnlockTarget(null)} onDone={setNotice} />
          ) : null}
          {adding ? <AddHrDialog companyId={company.id} onClose={() => setAdding(false)} onDone={setNotice} /> : null}
          {companyDialog === "lock" ? (
            <LockCompanyDialog company={company} onClose={() => setCompanyDialog(null)} onDone={setNotice} onConflict={setConflict} />
          ) : null}
          {companyDialog === "unlock" ? (
            <UnlockCompanyDialog company={company} onClose={() => setCompanyDialog(null)} onDone={setNotice} onConflict={setConflict} />
          ) : null}
        </>
      ) : null}
    </div>
  )
}

/** Một dòng thông tin trong thẻ bên phải; thiếu dữ liệu thì ghi "Chưa có" chứ không bỏ trống. */
function CompanyFact({ label, value }: { label: string; value: string | null }) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="break-words">{value ?? <span className="text-muted-foreground">Chưa có</span>}</dd>
    </div>
  )
}

export default function StaffCompanyDetailPage() {
  return (
    <React.Suspense fallback={<p className="text-sm text-muted-foreground">Đang tải...</p>}>
      <StaffCompanyDetailView />
    </React.Suspense>
  )
}
