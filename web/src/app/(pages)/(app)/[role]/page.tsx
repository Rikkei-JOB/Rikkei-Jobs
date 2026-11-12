"use client"

import * as React from "react"
import Link from "next/link"
import { notFound } from "next/navigation"
import { ROLE_MENU } from "@/config/menu"
import { ROLE_LABEL, roleFromSlug } from "@/config/roles"
import { useAuthStore } from "@/hooks/use-auth-store"

/**
 * Trang chủ của từng vai trò (/candidate, /hr, /mentor, /staff, /admin).
 * Đăng nhập, vai trò, thanh trên và menu do layout (app) lo.
 * Hiện lối tắt tới các mục trong menu của vai trò. Hồ sơ kỹ năng của ứng viên (HS-01) nằm ở mục "Hồ sơ" (/candidate/profile).
 */
export default function RoleHomePage({ params }: { params: Promise<{ role: string }> }) {
  const { role: slug } = React.use(params)
  const role = roleFromSlug(slug)
  const user = useAuthStore((s) => s.user)
  if (!role) notFound()
  if (!user) return null

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <div>
        <h1 className="text-lg font-semibold tracking-tight">Xin chào, {user.fullName}</h1>
        <p className="text-xs text-muted-foreground">
          {ROLE_LABEL[role]} · {user.email}
        </p>
      </div>
      <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {ROLE_MENU[role].map((item) => {
          const Icon = item.icon
          return (
            <li key={item.href}>
              <Link
                href={item.href}
                className="flex items-center gap-3 rounded-lg bg-card p-4 text-xs font-medium ring-1 ring-foreground/10 transition-colors outline-none hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring/30"
              >
                <Icon className="size-5 shrink-0 text-primary" aria-hidden />
                {item.label}
              </Link>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
