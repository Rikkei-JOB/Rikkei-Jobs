"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { ROLE_HOME } from "@/config/roles";
import { useAuthStore } from "@/hooks/use-auth-store";
import type { Role } from "@/interface/auth";

interface GuardOptions {
  /** Chỉ các vai trò này được vào trang. Bỏ trống = mọi vai trò đã đăng nhập. */
  roles?: Role[];
  /** Cho phép người dùng đang bị bắt đổi mật khẩu vào trang (chỉ trang đổi mật khẩu dùng). */
  allowMustChangePassword?: boolean;
}

/**
 * Chặn trang cần đăng nhập. Trả về true khi được phép hiển thị nội dung.
 * - chưa đăng nhập -> /login
 * - đang bị bắt đổi mật khẩu -> /change-password
 * - sai vai trò -> trang chủ của vai trò đó
 */
export function useAuthGuard({ roles, allowMustChangePassword = false }: GuardOptions = {}): boolean {
  const router = useRouter();
  const hasHydrated = useAuthStore((s) => s.hasHydrated);
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);

  const needsPasswordChange = Boolean(user?.mustChangePassword) && !allowMustChangePassword;
  const wrongRole = Boolean(user && roles && !roles.includes(user.role));
  const allowed = hasHydrated && Boolean(token) && Boolean(user) && !needsPasswordChange && !wrongRole;

  useEffect(() => {
    if (!hasHydrated) return;
    if (!token || !user) {
      router.replace("/login");
    } else if (needsPasswordChange) {
      router.replace("/change-password");
    } else if (wrongRole) {
      router.replace(ROLE_HOME[user.role]);
    }
  }, [hasHydrated, token, user, needsPasswordChange, wrongRole, router]);

  return allowed;
}

/** Dùng ở trang /login: đã đăng nhập rồi thì chuyển thẳng về trang chủ. */
export function useRedirectIfAuthenticated() {
  const router = useRouter();
  const hasHydrated = useAuthStore((s) => s.hasHydrated);
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);

  useEffect(() => {
    if (!hasHydrated || !token || !user) return;
    router.replace(user.mustChangePassword ? "/change-password" : ROLE_HOME[user.role]);
  }, [hasHydrated, token, user, router]);
}
