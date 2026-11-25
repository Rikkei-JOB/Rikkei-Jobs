"use client"

import * as React from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { CompanyHeader } from "@/app/components/company/company-header"
import { AddHrDialog } from "@/app/components/staff-company/add-hr-dialog"
import { HrAccountTable } from "@/app/components/staff-company/hr-account-table"
import { hrErrorMessage } from "@/app/components/staff-company/hr-error"
import { LockHrDialog } from "@/app/components/staff-company/lock-hr-dialog"
import { UnlockHrDialog } from "@/app/components/staff-company/unlock-hr-dialog"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useStaffCompany } from "@/hooks/use-staff-companies"
import type { StaffHrAccount } from "@/interface/company"

export default function StaffCompanyDetailPage() {
  const allowed = useAuthGuard({ roles: ["STAFF"] })
  const { id } = useParams<{ id: string }>()
  const { data: company, error, isPending, refetch } = useStaffCompany(id, allowed)

  const [lockTarget, setLockTarget] = React.useState<StaffHrAccount | null>(null)
  const [unlockTarget, setUnlockTarget] = React.useState<StaffHrAccount | null>(null)
  const [adding, setAdding] = React.useState(false)
  const [notice, setNotice] = React.useState("")

  if (!allowed) return null

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

      {company ? (
        <>
          <CompanyHeader
            company={company}
            subline={
              <>
                MST {company.taxCode} ·{" "}
                <span className={company.status === "LOCKED" ? "text-destructive" : "text-primary"}>
                  {company.status === "LOCKED" ? "Đã khoá" : "Hoạt động"}
                </span>
              </>
            }
            actions={
              <Button size="sm" onClick={() => setAdding(true)}>
                + Thêm HR
              </Button>
            }
          />

          <h2 className="text-sm font-semibold">Tài khoản HR ({company.hrAccounts.length})</h2>

          <HrAccountTable
            accounts={company.hrAccounts}
            onLock={(account) => {
              setNotice("")
              setLockTarget(account)
            }}
            onUnlock={(account) => {
              setNotice("")
              setUnlockTarget(account)
            }}
          />

          {lockTarget ? <LockHrDialog key={lockTarget.userId} account={lockTarget} onClose={() => setLockTarget(null)} onDone={setNotice} /> : null}
          {unlockTarget ? (
            <UnlockHrDialog key={unlockTarget.userId} account={unlockTarget} onClose={() => setUnlockTarget(null)} onDone={setNotice} />
          ) : null}
          {adding ? <AddHrDialog companyId={company.id} onClose={() => setAdding(false)} onDone={setNotice} /> : null}
        </>
      ) : null}
    </div>
  )
}
