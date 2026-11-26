"use client"

import * as React from "react"
import { useRouter } from "next/navigation"
import { ChevronRight, FlaskConical, GraduationCap, Loader2, ShieldCheck } from "lucide-react"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { ROLE_LABEL } from "@/config/roles"
import { env } from "@/config/env"
import type { Role } from "@/interface/auth"
import { getErrorMessage } from "@/services/api"
import { lmsMockService, MOCK_LMS_USERS } from "@/services/lms-mock.service"

/**
 * Ứng viên, mentor, cán bộ, quản trị đăng nhập qua LMS: LMS cấp vé SSO rồi chuyển về /sso-callback?ticket=...
 * Khi bật mock LMS (NEXT_PUBLIC_LMS_MOCK=true) có thêm danh sách tài khoản mẫu để đăng nhập thử;
 * nếu chưa cấu hình cổng LMS thật thì danh sách này là lối đăng nhập chính (không hiện nút bị vô hiệu).
 */
export function LmsLoginPanel() {
  const hasLms = Boolean(env.lmsLoginUrl)
  return (
    <div className="space-y-4">
      <p className="text-sm leading-relaxed text-muted-foreground">
        Dành cho sinh viên, ứng viên, thực tập sinh IOC, Mentor và cán bộ. Xác thực một cửa bằng tài khoản LMS, không
        cần tạo mật khẩu mới.
      </p>
      {hasLms || !env.lmsMockEnabled ? (
        <Button type="button" size="lg" className="w-full" disabled={!hasLms} onClick={() => window.location.assign(env.lmsLoginUrl)}>
          <GraduationCap aria-hidden="true" />
          Đăng nhập bằng LMS
        </Button>
      ) : null}
      {!hasLms && !env.lmsMockEnabled ? (
        <Alert>Cổng LMS chưa được cấu hình. Vui lòng liên hệ quản trị hệ thống.</Alert>
      ) : null}
      {env.lmsMockEnabled ? <MockLmsUsers /> : null}
      <p className="flex items-center justify-center gap-1.5 text-xs text-muted-foreground">
        <ShieldCheck className="size-3.5 text-[#15803d]" aria-hidden="true" />
        Thông tin đăng nhập do LMS xác thực, Rikkei Jobs không lưu mật khẩu LMS.
      </p>
    </div>
  )
}

const ROLE_TONE: Partial<Record<Role, string>> = {
  CANDIDATE: "bg-[#fff1f2] text-[#b4232d]",
  MENTOR: "bg-[#f5f3ff] text-[#6d28d9]",
  STAFF: "bg-[#fffbeb] text-[#b45309]",
  ADMIN: "bg-[#f1f5f9] text-[#334155]",
}
const initials = (name: string) => name.trim().split(/\s+/).slice(-2).map((part) => part[0]).join("").toUpperCase()

function MockLmsUsers() {
  const router = useRouter()
  const [error, setError] = React.useState<string | null>(null)
  const [pending, setPending] = React.useState<string | null>(null)

  async function signIn(lmsUserId: string) {
    setPending(lmsUserId)
    setError(null)
    try {
      const ticket = await lmsMockService.issueTicket(lmsUserId)
      router.push(`/sso-callback?ticket=${encodeURIComponent(ticket)}`)
    } catch (e) {
      setError(getErrorMessage(e, "Không lấy được vé từ mock LMS."))
      setPending(null)
    }
  }

  return (
    <section aria-labelledby="mock-lms-title" className="overflow-hidden rounded-xl border border-border">
      <div className="flex items-center gap-2 border-b border-border bg-[#f8fafc] px-3.5 py-2.5">
        <FlaskConical className="size-4 text-[#b45309]" aria-hidden="true" />
        <h3 id="mock-lms-title" className="text-xs font-semibold text-heading">Tài khoản LMS mẫu</h3>
        <span className="ml-auto rounded-md bg-[#fffbeb] px-1.5 py-0.5 text-[11px] font-semibold text-[#b45309]">Môi trường phát triển</span>
      </div>
      {error ? <Alert variant="destructive" className="m-3">{error}</Alert> : null}
      <ul className="divide-y divide-border bg-white">
        {MOCK_LMS_USERS.map((u) => (
          <li key={u.lmsUserId}>
            <button
              type="button"
              className="flex min-h-14 w-full items-center gap-3 px-3.5 py-2 text-left transition-colors hover:bg-[#fff8f8] focus-visible:bg-[#fff8f8] focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-[#b4232d] disabled:cursor-not-allowed disabled:opacity-60"
              disabled={pending !== null}
              onClick={() => signIn(u.lmsUserId)}
              aria-label={`Đăng nhập thử bằng ${u.fullName}, vai trò ${ROLE_LABEL[u.role]}`}
            >
              <span className={`flex size-9 shrink-0 items-center justify-center rounded-full text-xs font-bold ${ROLE_TONE[u.role] ?? ""}`} aria-hidden="true">
                {initials(u.fullName)}
              </span>
              <span className="min-w-0 flex-1">
                <strong className="block truncate text-sm font-semibold text-heading">{u.fullName}</strong>
                <span className="block truncate text-xs text-muted-foreground">{u.email}</span>
                {/* Màn hẹp: nhãn vai trò xuống dưới để tên không bị cắt. */}
                <span className={`mt-1 hidden rounded-full px-2 py-0.5 text-[11px] font-semibold max-[479px]:inline-block ${ROLE_TONE[u.role] ?? ""}`}>{ROLE_LABEL[u.role]}</span>
              </span>
              <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-[11px] font-semibold max-[479px]:hidden ${ROLE_TONE[u.role] ?? ""}`}>{ROLE_LABEL[u.role]}</span>
              {pending === u.lmsUserId ? (
                <Loader2 className="size-4 shrink-0 animate-spin text-[#b4232d]" aria-hidden="true" />
              ) : (
                <ChevronRight className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
              )}
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
