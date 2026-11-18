"use client"

import Link from "next/link"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { JobForm } from "@/app/components/job/job-form"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useMyCompany } from "@/hooks/use-my-company"
import { useSkillCatalog } from "@/hooks/use-skill-catalog"
import { getErrorMessage } from "@/services/api"

/** Tạo JD mới; lần lưu đầu chuyển sang /hr/jobs/{id}. */
export default function NewJobPage() {
  const allowed = useAuthGuard({ roles: ["HR"] })
  const company = useMyCompany(allowed)
  const catalog = useSkillCatalog(allowed)

  if (!allowed) return null

  const error = company.error ?? catalog.error
  if (error) {
    return (
      <div className="mx-auto max-w-3xl">
        <Alert variant="destructive" className="flex items-center justify-between gap-2">
          <span>{getErrorMessage(error)}</span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              if (company.error) void company.refetch()
              if (catalog.error) void catalog.refetch()
            }}
          >
            Thử lại
          </Button>
        </Alert>
      </div>
    )
  }

  if (!company.data || !catalog.data) return <p className="text-xs text-muted-foreground">Đang tải...</p>

  if (company.data.status === "LOCKED") {
    return (
      <div className="mx-auto max-w-3xl space-y-3">
        <Alert>Công ty đang bị khoá, không tạo được JD.</Alert>
        <Button variant="outline" size="sm" render={<Link href="/hr/jobs" />} nativeButton={false}>
          Quay lại danh sách
        </Button>
      </div>
    )
  }

  return <JobForm companyLocked={false} />
}
