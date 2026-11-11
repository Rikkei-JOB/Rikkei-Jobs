"use client"

import * as React from "react"
import Link from "next/link"
import { notFound, useRouter } from "next/navigation"
import { Button } from "@/app/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card"
import { ROLE_LABEL, roleFromSlug } from "@/config/roles"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useAuthStore } from "@/hooks/use-auth-store"
import { authService } from "@/services/auth.service"

/**
 * Trang chủ tạm của từng vai trò (/candidate, /hr, /mentor, /staff, /admin) sau khi đăng nhập.
 * Các task khác thay bằng trang thật; chỉ giữ quy ước: trang nằm đúng đường dẫn ROLE_HOME và dùng useAuthGuard.
 */
export default function RoleHomePage({ params }: { params: Promise<{ role: string }> }) {
  const { role: slug } = React.use(params)
  const role = roleFromSlug(slug)
  if (!role) notFound()

  return <RoleHome expectedRole={role} />
}

function RoleHome({ expectedRole }: { expectedRole: NonNullable<ReturnType<typeof roleFromSlug>> }) {
  const router = useRouter()
  const allowed = useAuthGuard({ roles: [expectedRole] })
  const user = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)
  const clear = useAuthStore((s) => s.clear)

  // Làm mới thông tin người dùng từ máy chủ; token hết hạn sẽ tự đưa về /login (xem services/api.ts).
  React.useEffect(() => {
    if (!allowed) return
    authService.me().then(setUser).catch(() => undefined)
  }, [allowed, setUser])

  if (!allowed || !user) return null

  function logout() {
    clear()
    router.replace("/login")
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="text-base">Xin chào, {user.fullName}</CardTitle>
          <CardDescription>
            {ROLE_LABEL[user.role]} · {user.email}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <p className="text-xs/relaxed text-muted-foreground">
            Đây là trang chủ tạm của vai trò này. Chức năng của vai trò sẽ được bổ sung ở các task sau.
          </p>
          <div className="flex gap-2">
            {user.role === "HR" ? (
              <Button variant="outline" render={<Link href="/change-password" />} nativeButton={false}>
                Đổi mật khẩu
              </Button>
            ) : null}
            <Button variant="outline" onClick={logout}>
              Đăng xuất
            </Button>
          </div>
        </CardContent>
      </Card>
    </main>
  )
}
