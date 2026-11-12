"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { ROLE_HOME } from "@/config/roles";
import { pathAfterLogin, rememberNextPath } from "@/helper/next-path";
import { useAuthStore } from "@/hooks/use-auth-store";
import type { Role } from "@/interface/auth";

interface GuardOptions {
  /** Chỉ các vai trò này được vào trang. Bỏ trống = mọi vai trò đã đăng nhập. */
  roles?: Role[];
  /** Cho phép người dùng đang bị bắt đổi mật khẩu vào trang (chỉ trang đổi mật khẩu dùng). */
  allowMustChangePassword?: boolean;
}

/**
 * Chặn trang cần đăng nhập nằm ngoài layout (app), ví dụ /change-password. Trả về true khi được phép hiển thị.
 * - chưa đăng nhập -> /login
 * - đang bị bắt đổi mật khẩu -> /change-password
 * - sai vai trò -> trang chủ của vai trò đó
 *
 * Trang trong (app) không cần gọi hook này: layout (app) đã chặn bằng {@link useRoleAccess}.
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

export type RoleAccess = "pending" | "allowed" | "forbidden";

/**
 * Route guard của layout (app) (NT-02):
 * - chưa đăng nhập -> nhớ trang đang mở rồi chuyển /login ("pending")
 * - đang bị bắt đổi mật khẩu -> /change-password ("pending")
 * - đường dẫn thuộc vai trò khác -> "forbidden" (layout hiện trang 403, giữ nguyên URL)
 *
 * Chỉ để trải nghiệm tốt hơn; quyền thật được kiểm ở API.
 */
export function useRoleAccess(requiredRole: Role | null): RoleAccess {
  const router = useRouter();
  const pathname = usePathname();
  const hasHydrated = useAuthStore((s) => s.hasHydrated);
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);

  const signedIn = Boolean(token && user);
  const needsPasswordChange = Boolean(user?.mustChangePassword);

  useEffect(() => {
    if (!hasHydrated) return;
    if (!signedIn) {
      rememberNextPath(pathname);
      router.replace("/login");
    } else if (needsPasswordChange) {
      router.replace("/change-password");
    }
  }, [hasHydrated, signedIn, needsPasswordChange, pathname, router]);

  if (!hasHydrated || !signedIn || needsPasswordChange || !user) return "pending";
  if (requiredRole && user.role !== requiredRole) return "forbidden";
  return "allowed";
}

/** Dùng ở trang /login: đã đăng nhập rồi thì chuyển thẳng về trang định mở hoặc trang chủ. */
export function useRedirectIfAuthenticated() {
  const router = useRouter();
  const hasHydrated = useAuthStore((s) => s.hasHydrated);
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);

  useEffect(() => {
    if (!hasHydrated || !token || !user) return;
    router.replace(user.mustChangePassword ? "/change-password" : pathAfterLogin(user.role));
  }, [hasHydrated, token, user, router]);
}
