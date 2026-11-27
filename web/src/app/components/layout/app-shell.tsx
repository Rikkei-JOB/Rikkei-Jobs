"use client"

import * as React from "react"
import { useLogout } from "@/hooks/use-logout"
import type { User } from "@/interface/auth"
import { CandidateShell } from "./candidate-shell"
import { HrShell } from "./hr-shell"
import { AdminShell, MentorShell, StaffShell } from "./role-shells"

/** Khung chung sau đăng nhập: chọn thanh điều hướng theo vai trò. */
export function AppShell({ user, children }: { user: User; children: React.ReactNode }) {
  // Xoá cả phiên lẫn cache truy vấn (react-query), để người đăng nhập sau không thấy dữ liệu của người trước.
  const logout = useLogout()

  switch (user.role) {
    case "CANDIDATE":
      return <CandidateShell>{children}</CandidateShell>
    // Mỗi vai trò làm việc một bố cục (cùng nền sáng): HR thanh ngang (UT-02), Mentor sidebar,
    // cán bộ thanh trên + rail icon, quản trị sidebar nhóm mục + breadcrumb (role-shells.tsx).
    case "HR":
      return <HrShell user={user} onLogout={logout}>{children}</HrShell>
    case "STAFF":
      return <StaffShell user={user} onLogout={logout}>{children}</StaffShell>
    case "MENTOR":
      return <MentorShell user={user} onLogout={logout}>{children}</MentorShell>
    case "ADMIN":
      return <AdminShell user={user} onLogout={logout}>{children}</AdminShell>
  }
}
