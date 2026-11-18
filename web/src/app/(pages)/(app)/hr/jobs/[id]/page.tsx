"use client"

import * as React from "react"
import Link from "next/link"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { JobForm } from "@/app/components/job/job-form"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useMyCompany } from "@/hooks/use-my-company"
import { useJob } from "@/hooks/use-my-jobs"
import { useSkillCatalog } from "@/hooks/use-skill-catalog"
import { ApiError, getErrorMessage } from "@/services/api"

/** Sửa hoặc xem một JD. Next 16: `params` là Promise, trang client đọc bằng React.use(). */
export default function JobDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = React.use(params)
  const allowed = useAuthGuard({ roles: ["HR"] })
  const job = useJob(id, allowed)
  const company = useMyCompany(allowed)
  const catalog = useSkillCatalog(allowed)

  if (!allowed) return null

  // 404 = không có hoặc thuộc công ty khác; 400 = id sai định dạng UUID.
  if (job.error instanceof ApiError && (job.error.status === 404 || job.error.status === 400)) {
    return (
      <div className="mx-auto max-w-3xl space-y-3">
        <Alert variant="destructive">Không tìm thấy JD</Alert>
        <Button variant="outline" size="sm" render={<Link href="/hr/jobs" />} nativeButton={false}>
          Quay lại danh sách
        </Button>
      </div>
    )
  }

  const ready = job.data && company.data && catalog.data
  const error = job.error ?? company.error ?? catalog.error
  // Lỗi khi tải lại nền (đã có dữ liệu) thì vẫn giữ form.
  if (error && !ready) {
    return (
      <div className="mx-auto max-w-3xl">
        <Alert variant="destructive" className="flex items-center justify-between gap-2">
          <span>{getErrorMessage(error)}</span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              if (job.error) void job.refetch()
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

  if (!job.data || !company.data || !catalog.data) return <p className="text-xs text-muted-foreground">Đang tải...</p>

  return <JobForm key={job.data.id} job={job.data} companyLocked={company.data.status === "LOCKED"} />
}
