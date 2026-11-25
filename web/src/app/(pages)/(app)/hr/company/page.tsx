"use client"

import * as React from "react"
import Link from "next/link"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { CompanyHeader } from "@/app/components/company/company-header"
import { CompanyOpenJobs } from "@/app/components/company/company-open-jobs"
import { CompanyProfileForm, type CompanyTab } from "@/app/components/company/company-profile-form"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useMyCompany } from "@/hooks/use-my-company"
import { useOpenJobs } from "@/hooks/use-my-jobs"
import { getErrorMessage } from "@/services/api"

/** Hồ sơ công ty của HR: đầu trang + 3 tab; hai tab đầu dùng chung một form với thanh lưu dính đáy. */
export default function HrCompanyPage() {
  const allowed = useAuthGuard({ roles: ["HR"] })
  const { data: company, error, isPending, refetch } = useMyCompany(allowed)
  const { data: openJobs = [] } = useOpenJobs(allowed)
  const [tab, setTab] = React.useState<CompanyTab>("intro")

  if (!allowed) return null

  const locked = company?.status === "LOCKED"
  const tabs: { id: CompanyTab; label: string }[] = [
    { id: "intro", label: "Giới thiệu" },
    { id: "contact", label: "Thông tin liên hệ" },
    { id: "jobs", label: `Việc làm đang tuyển (${openJobs.length})` },
  ]

  return (
    <div className="space-y-4">
      {isPending ? <p className="text-xs text-muted-foreground">Đang tải...</p> : null}

      {error ? (
        <Alert variant="destructive" className="flex items-center justify-between gap-2">
          <span>{getErrorMessage(error)}</span>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Thử lại
          </Button>
        </Alert>
      ) : null}

      {company ? (
        <>
          <CompanyHeader
            company={company}
            subline={`MST ${company.taxCode} · ${company.status === "ACTIVE" ? "Đang hoạt động" : "Đang bị khoá"} · Liên hệ cán bộ để đổi`}
            actions={
              <Button size="sm" render={<Link href="/hr/company/preview" />} nativeButton={false}>
                Xem như ứng viên
              </Button>
            }
            tabs={
              <div role="tablist" aria-label="Hồ sơ công ty" className="flex gap-1">
                {tabs.map((t) => (
                  <button
                    key={t.id}
                    type="button"
                    role="tab"
                    aria-selected={tab === t.id}
                    onClick={() => setTab(t.id)}
                    className={`-mb-px border-b-2 px-3 py-2.5 text-xs font-medium whitespace-nowrap ${
                      tab === t.id
                        ? "border-primary text-primary"
                        : "border-transparent text-muted-foreground hover:text-foreground"
                    }`}
                  >
                    {t.label}
                  </button>
                ))}
              </div>
            }
          />
          {locked ? <Alert>Công ty đang bị khoá, bạn chỉ xem được hồ sơ.</Alert> : null}
          <CompanyProfileForm company={company} disabled={locked} tab={tab} onTabChange={setTab} />
          {tab === "jobs" ? <CompanyOpenJobs jobs={openJobs} locked={locked} /> : null}
        </>
      ) : null}
    </div>
  )
}
