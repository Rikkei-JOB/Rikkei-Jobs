"use client"

import Link from "next/link"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { CompanyPublicView } from "@/app/components/company/company-public-view"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useMyCompany } from "@/hooks/use-my-company"
import { getErrorMessage } from "@/services/api"

/** Xem trước trang công ty như ứng viên thấy; luôn tải lại bản đã lưu, không dùng bản cũ trong cache. */
export default function HrCompanyPreviewPage() {
  const allowed = useAuthGuard({ roles: ["HR"] })
  const { data: company, error, isFetching, refetch } = useMyCompany(allowed, { refetchOnMount: "always" })

  if (!allowed) return null

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-border bg-muted/50 px-3 py-2 text-xs/relaxed">
        <span>Bạn đang xem trang công ty như ứng viên thấy</span>
        <Button variant="outline" size="sm" render={<Link href="/hr/company" />} nativeButton={false}>
          ‹ Quay lại chỉnh sửa
        </Button>
      </div>

      {isFetching ? <p className="text-xs text-muted-foreground">Đang tải...</p> : null}

      {error ? (
        <Alert variant="destructive" className="flex items-center justify-between gap-2">
          <span>{getErrorMessage(error)}</span>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Thử lại
          </Button>
        </Alert>
      ) : null}

      {company && !isFetching ? <CompanyPublicView company={company} /> : null}
    </div>
  )
}
