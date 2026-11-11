"use client"

import * as React from "react"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { Loader2 } from "lucide-react"
import { AuthCard } from "@/app/components/auth/auth-card"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { ROLE_HOME } from "@/config/roles"
import { useAuthStore } from "@/hooks/use-auth-store"
import { getErrorMessage } from "@/services/api"
import { authService } from "@/services/auth.service"

function SsoCallback() {
  const router = useRouter()
  const ticket = useSearchParams().get("ticket")
  const setSession = useAuthStore((s) => s.setSession)
  const [error, setError] = React.useState<string | null>(ticket ? null : "Thiếu vé đăng nhập từ LMS.")
  // Vé SSO chỉ dùng được một lần: chặn việc gọi hai lần (React Strict Mode chạy effect hai lần khi dev).
  const started = React.useRef(false)

  React.useEffect(() => {
    if (!ticket || started.current) return
    started.current = true
    authService
      .sso(ticket)
      .then((result) => {
        setSession(result.accessToken, result.user)
        router.replace(ROLE_HOME[result.user.role])
      })
      .catch((e) => setError(getErrorMessage(e)))
  }, [ticket, router, setSession])

  if (error) {
    return (
      <AuthCard title="Không đăng nhập được">
        <Alert variant="destructive">{error}</Alert>
        <p className="text-xs/relaxed text-muted-foreground">
          Vé đăng nhập chỉ có hiệu lực 60 giây và dùng được một lần. Hãy đăng nhập lại từ đầu.
        </p>
        <Button size="lg" className="w-full" render={<Link href="/login" />} nativeButton={false}>
          Quay lại trang đăng nhập
        </Button>
      </AuthCard>
    )
  }

  return (
    <AuthCard title="Đang đăng nhập...">
      <div className="flex items-center justify-center gap-2 py-4 text-xs/relaxed text-muted-foreground">
        <Loader2 className="size-4 animate-spin" aria-hidden />
        Đang xác thực với LMS
      </div>
    </AuthCard>
  )
}

export default function SsoCallbackPage() {
  return (
    <React.Suspense fallback={null}>
      <SsoCallback />
    </React.Suspense>
  )
}
