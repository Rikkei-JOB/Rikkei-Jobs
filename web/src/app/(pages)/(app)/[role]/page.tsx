"use client"

import * as React from "react"
import Link from "next/link"
import { notFound } from "next/navigation"
import { Button } from "@/app/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card"
import { ROLE_LABEL, roleFromSlug } from "@/config/roles"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useAuthStore } from "@/hooks/use-auth-store"
import { useLogout } from "@/hooks/use-logout"
import { authService } from "@/services/auth.service"
import { SkillProfile } from "@/app/components/skills/skill-profile"
import { Building2, LogOut, User, KeyRound } from "lucide-react"

/**
 * Trang chủ của từng vai trò (/candidate, /hr, /mentor, /staff, /admin) sau khi đăng nhập.
 * Ứng viên (CANDIDATE) hiển thị toàn bộ Hồ sơ năng lực kỹ năng (Skill Profile - HS-01).
 */
export default function RoleHomePage({ params }: { params: Promise<{ role: string }> }) {
  const { role: slug } = React.use(params)
  const role = roleFromSlug(slug)
  if (!role) notFound()

  return <RoleHome expectedRole={role} />
}

function RoleHome({ expectedRole }: { expectedRole: NonNullable<ReturnType<typeof roleFromSlug>> }) {
  const allowed = useAuthGuard({ roles: [expectedRole] })
  const user = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)
  const logout = useLogout()

  // Làm mới thông tin người dùng từ máy chủ; token hết hạn sẽ tự đưa về /login (xem services/api.ts).
  React.useEffect(() => {
    if (!allowed) return
    authService.me().then(setUser).catch(() => undefined)
  }, [allowed, setUser])

  if (!allowed || !user) return null

  // Đối với ứng viên: hiển thị trực tiếp Hồ sơ năng lực kỹ năng (HS-01)
  if (user.role === "CANDIDATE") {
    return (
      <div className="min-h-screen bg-muted/20">
        {/* Top Navbar */}
        <header className="sticky top-0 z-40 border-b bg-background/95 backdrop-blur-sm">
          <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4 sm:px-6">
            <div className="flex items-center gap-3">
              <span className="font-heading text-base font-bold text-primary">Rikkei Jobs</span>
              <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-[11px] font-medium text-primary">
                Ứng viên
              </span>
            </div>

            <div className="flex items-center gap-3 text-xs">
              <div className="hidden text-right sm:block">
                <div className="font-medium text-foreground">{user.fullName}</div>
                <div className="text-[11px] text-muted-foreground">{user.email}</div>
              </div>
              <Button variant="outline" size="sm" onClick={logout}>
                <LogOut className="mr-1.5 h-3.5 w-3.5" />
                Đăng xuất
              </Button>
            </div>
          </div>
        </header>

        {/* Main Content Area */}
        <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
          <SkillProfile />
        </main>
      </div>
    )
  }

  // Các vai trò khác: giữ màn hình khung phân quyền
  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-primary">
              <User className="h-4 w-4" />
            </div>
            <div>
              <CardTitle className="text-base">Xin chào, {user.fullName}</CardTitle>
              <CardDescription>
                {ROLE_LABEL[user.role]} · {user.email}
              </CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-xs/relaxed text-muted-foreground">
            Đây là trang chủ của vai trò {ROLE_LABEL[user.role]}. Các tính năng tương ứng sẽ được phát triển theo task.
          </p>
          <div className="flex gap-2">
            {user.role === "HR" ? (
              <>
                <Button variant="outline" render={<Link href="/hr/company" />} nativeButton={false}>
                  <Building2 className="mr-1.5 h-3.5 w-3.5" />
                  Hồ sơ công ty
                </Button>
                <Button variant="outline" render={<Link href="/change-password" />} nativeButton={false}>
                  <KeyRound className="mr-1.5 h-3.5 w-3.5" />
                  Đổi mật khẩu
                </Button>
              </>
            ) : null}
            <Button variant="outline" onClick={logout}>
              <LogOut className="mr-1.5 h-3.5 w-3.5" />
              Đăng xuất
            </Button>
          </div>
        </CardContent>
      </Card>
    </main>
  )
}
