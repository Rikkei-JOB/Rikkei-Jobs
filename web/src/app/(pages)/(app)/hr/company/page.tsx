"use client"

import * as React from "react"
import Link from "next/link"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { CompanyProfileForm } from "@/app/components/company/company-profile-form"
import { CompanySummaryCard } from "@/app/components/company/company-summary-card"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useMyCompany, useUploadMyLogo } from "@/hooks/use-my-company"
import { getErrorMessage } from "@/services/api"

/** Hồ sơ công ty của HR: thẻ tóm tắt cố định bên trái, form chỉnh sửa bên phải (một cột trên màn hẹp). */
export default function HrCompanyPage() {
  const allowed = useAuthGuard({ roles: ["HR"] })
  const { data: company, error, isPending, refetch } = useMyCompany(allowed)
  const uploadLogo = useUploadMyLogo()
  const [logoError, setLogoError] = React.useState<string | null>(null)
  const [dirty, setDirty] = React.useState(false)

  if (!allowed) return null

  // File đã được thẻ tóm tắt kiểm tra (định dạng, dung lượng) trước khi hiện xem trước.
  function handleLogoUpload(file: File, onSuccess: () => void) {
    setLogoError(null)
    uploadLogo.mutate(file, { onSuccess, onError: (e) => setLogoError(getErrorMessage(e)) })
  }

  const locked = company?.status === "LOCKED"

  return (
    // Nằm trong <main> của layout (app) (NT-02), nên dùng <div> và không thêm padding.
    <div className="hr-page">
      <div className="hr-page-heading">
        <div><p className="hr-eyebrow">THÔNG TIN DOANH NGHIỆP</p><h1>Hồ sơ công ty</h1><p className="hr-description">Giới thiệu doanh nghiệp để ứng viên hiểu hơn về nơi làm việc.</p></div>
        <div className="flex flex-col items-end gap-1">
          <div className="flex gap-2">
            {company ? (
              <Button size="sm" render={<Link href="/hr/company/preview" />} nativeButton={false}>
                Xem như ứng viên
              </Button>
            ) : null}
            <Button variant="outline" size="sm" render={<Link href="/hr" />} nativeButton={false}>
              Quay lại
            </Button>
          </div>
          {dirty ? <p className="text-xs text-muted-foreground">Xem trước chỉ hiện bản đã lưu</p> : null}
        </div>
      </div>

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
          {locked ? <Alert>Công ty đang bị khoá, bạn chỉ xem được hồ sơ.</Alert> : null}
          <div className="grid min-w-0 gap-6 min-[1200px]:grid-cols-[280px_minmax(0,1fr)] min-[1200px]:items-start">
            <CompanySummaryCard
              company={company}
              onLogoUpload={handleLogoUpload}
              uploading={uploadLogo.isPending}
              error={logoError}
              disabled={locked}
            />
            <CompanyProfileForm
              company={company}
              disabled={locked}
              onSaved={() => undefined}
              onDirtyChange={setDirty}
            />
          </div>
        </>
      ) : null}
    </div>
  )
}
