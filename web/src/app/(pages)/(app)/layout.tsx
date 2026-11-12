"use client"

import * as React from "react"
import { usePathname } from "next/navigation"
import { AppShell } from "@/app/components/layout/app-shell"
import { Forbidden } from "@/app/components/layout/forbidden"
import { roleFromSlug } from "@/config/roles"
import { useRoleAccess } from "@/hooks/use-auth-guard"
import { useAuthStore } from "@/hooks/use-auth-store"
import { authService } from "@/services/auth.service"

/**
 * Layout chung cho mọi trang sau đăng nhập (/candidate/..., /hr/..., /mentor/..., /staff/..., /admin/...).
 * Đoạn đầu của đường dẫn quyết định vai trò được vào; route guard ở {@link useRoleAccess}.
 * Trang con không cần tự kiểm đăng nhập hay vai trò.
 */
export default function AppLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const requiredRole = roleFromSlug(pathname.split("/")[1] ?? "")
  const access = useRoleAccess(requiredRole)
  const user = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)
  const signedIn = access !== "pending"

  // Làm mới thông tin người dùng một lần; token hết hạn sẽ tự đưa về /login (services/api.ts).
  React.useEffect(() => {
    if (!signedIn) return
    authService.me().then(setUser).catch(() => undefined)
  }, [signedIn, setUser])

  if (access === "pending" || !user) return null

  return <AppShell user={user}>{access === "forbidden" ? <Forbidden role={user.role} /> : children}</AppShell>
}
