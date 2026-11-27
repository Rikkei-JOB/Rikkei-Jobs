"use client"

import * as React from "react"
import Link from "next/link"
import { useParams, useRouter, useSearchParams } from "next/navigation"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { CompanyHeader } from "@/app/components/company/company-header"
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
import type { StaffHrAccount } from "@/interface/company"
import type { StaffJobStatus } from "@/interface/staff-job"

/** Các tab của trang; tab đang chọn nằm trên URL (?tab=hr|jobs|history), mặc định hr. */
const TABS = ["hr", "jobs", "history"] as const
type DetailTab = (typeof TABS)[number]

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

  const tabs: { id: DetailTab; label: string }[] = [
    { id: "hr", label: company ? `Tài khoản HR (${company.hrAccounts.length})` : "Tài khoản HR" },
    { id: "jobs", label: "JD của công ty" },
    { id: "history", label: "Lịch sử thao tác" },
  ]
  const locked = company?.status === "LOCKED"

  return (
    <div className="space-y-4">
      <Link href="/staff/companies" className="text-xs text-primary hover:underline">
        ‹ Doanh nghiệp
      </Link>

      {isPending ? <p className="text-xs text-muted-foreground">Đang tải...</p> : null}

      {error ? (
        <Alert variant="destructive" className="flex items-center justify-between gap-2">
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
          <CompanyHeader
            company={company}
            subline={
              <>
                MST {company.taxCode} ·{" "}
                <span className={locked ? "text-destructive" : "text-green-700"}>{locked ? "Đã khoá" : "Hoạt động"}</span>
              </>
            }
            actions={
              <>
                <Button size="sm" onClick={() => setAdding(true)}>
                  + Thêm HR
                </Button>
                {locked ? (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      clearMessages()
                      setCompanyDialog("unlock")
                    }}
                  >
                    Mở khoá
                  </Button>
                ) : (
                  <Button
                    variant="outline"
                    size="sm"
                    className="border-destructive text-destructive hover:bg-destructive/10 hover:text-destructive"
                    onClick={() => {
                      clearMessages()
                      setCompanyDialog("lock")
                    }}
                  >
                    Khoá công ty
                  </Button>
                )}
              </>
            }
            tabs={
              <div role="tablist" aria-label="Chi tiết doanh nghiệp" className="flex gap-1">
                {tabs.map((t) => (
                  <button
                    key={t.id}
                    id={`company-tab-${t.id}`}
                    type="button"
                    role="tab"
                    aria-selected={tab === t.id}
                    aria-controls={`company-panel-${t.id}`}
                    onClick={() => selectTab(t.id)}
                    className={`-mb-px border-b-2 px-3 py-2.5 text-xs font-medium whitespace-nowrap ${
                      tab === t.id ? "border-primary text-primary" : "border-transparent text-muted-foreground hover:text-foreground"
                    }`}
                  >
                    {t.label}
                  </button>
                ))}
              </div>
            }
          />

          {locked ? <CompanyLockBanner lock={company.lock} /> : null}

          <div role="tabpanel" id={`company-panel-${tab}`} aria-labelledby={`company-tab-${tab}`}>
            {tab === "hr" ? (
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
            ) : tab === "jobs" ? (
              <CompanyJobsTable companyId={company.id} status={jobsView.status} page={jobsView.page} onChange={setJobsView} />
            ) : (
              <CompanyHistoryTable companyId={company.id} page={historyPage} onPage={setHistoryPage} />
            )}
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

export default function StaffCompanyDetailPage() {
  return (
    <React.Suspense fallback={<p className="text-xs text-muted-foreground">Đang tải...</p>}>
      <StaffCompanyDetailView />
    </React.Suspense>
  )
}
