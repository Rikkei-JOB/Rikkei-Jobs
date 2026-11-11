"use client"

import * as React from "react"
import { useRouter } from "next/navigation"
import { Loader2 } from "lucide-react"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { ROLE_LABEL } from "@/config/roles"
import { env } from "@/config/env"
import { getErrorMessage } from "@/services/api"
import { lmsMockService, MOCK_LMS_USERS } from "@/services/lms-mock.service"

/**
 * Ứng viên, mentor, cán bộ, quản trị đăng nhập qua LMS: LMS cấp vé SSO rồi chuyển về /sso-callback?ticket=...
 * Khi bật mock LMS (NEXT_PUBLIC_LMS_MOCK=true) có thêm danh sách tài khoản mẫu để đăng nhập thử.
 */
export function LmsLoginPanel() {
  return (
    <div className="space-y-4">
      <p className="text-xs/relaxed text-muted-foreground">
        Dùng tài khoản LMS của bạn (sinh viên, học viên, thực tập sinh IOC, mentor, cán bộ). Bạn sẽ được chuyển sang LMS
        để đăng nhập, sau đó quay lại đây.
      </p>
      <Button
        type="button"
        size="lg"
        className="w-full"
        disabled={!env.lmsLoginUrl}
        onClick={() => window.location.assign(env.lmsLoginUrl)}
      >
        Đăng nhập bằng LMS
      </Button>
      {!env.lmsLoginUrl ? (
        <p className="text-center text-xs/relaxed text-muted-foreground">
          Chưa cấu hình địa chỉ đăng nhập LMS (NEXT_PUBLIC_LMS_LOGIN_URL).
        </p>
      ) : null}
      {env.lmsMockEnabled ? <MockLmsUsers /> : null}
    </div>
  )
}

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
    <div className="space-y-2 rounded-lg border border-dashed p-3">
      <p className="text-xs/relaxed font-medium">Mock LMS (chỉ dùng khi phát triển, cần chạy mock/ ở cổng 9000)</p>
      {error ? <Alert variant="destructive">{error}</Alert> : null}
      <ul className="space-y-1.5">
        {MOCK_LMS_USERS.map((u) => (
          <li key={u.lmsUserId}>
            <Button
              type="button"
              variant="outline"
              className="w-full justify-between"
              disabled={pending !== null}
              onClick={() => signIn(u.lmsUserId)}
            >
              <span>
                {u.email.split("@")[0]} · {u.fullName}
              </span>
              <span className="text-muted-foreground">
                {pending === u.lmsUserId ? <Loader2 className="animate-spin" aria-hidden /> : ROLE_LABEL[u.role]}
              </span>
            </Button>
          </li>
        ))}
      </ul>
    </div>
  )
}
