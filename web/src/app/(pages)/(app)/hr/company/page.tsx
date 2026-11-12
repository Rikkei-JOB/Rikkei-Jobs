"use client"

import * as React from "react"
import Link from "next/link"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { CompanyProfileForm } from "@/app/components/company/company-profile-form"
import { CompanySummaryCard } from "@/app/components/company/company-summary-card"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useMyCompany, useUploadMyLogo } from "@/hooks/use-my-company"
import { validateLogoFile } from "@/schemas/company.schema"
import { getErrorMessage } from "@/services/api"

/** Hồ sơ công ty của HR: thẻ tóm tắt cố định bên trái, form chỉnh sửa bên phải (một cột trên màn hẹp). */
export default function HrCompanyPage() {
  const allowed = useAuthGuard({ roles: ["HR"] })
  const { data: company, error, isPending, refetch } = useMyCompany(allowed)
  const uploadLogo = useUploadMyLogo()
  const [logoError, setLogoError] = React.useState<string | null>(null)

  if (!allowed) return null

  function handleLogoChange(file: File) {
    const invalid = validateLogoFile(file)
    setLogoError(invalid)
    if (invalid) return
    uploadLogo.mutate(file, { onError: (e) => setLogoError(getErrorMessage(e)) })
  }

  const locked = company?.status === "LOCKED"

  return (
    <main className="mx-auto max-w-5xl space-y-4 px-4 py-8 sm:px-6">
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-lg font-semibold">Hồ sơ công ty</h1>
        <Button variant="outline" size="sm" render={<Link href="/hr" />} nativeButton={false}>
          Quay lại
        </Button>
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
          <div className="grid gap-4 md:grid-cols-[280px_1fr] md:items-start">
            <CompanySummaryCard
              company={company}
              onLogoChange={handleLogoChange}
              uploading={uploadLogo.isPending}
              error={logoError}
              disabled={locked}
            />
            <CompanyProfileForm company={company} disabled={locked} onSaved={() => undefined} />
          </div>
        </>
      ) : null}
    </main>
  )
}
