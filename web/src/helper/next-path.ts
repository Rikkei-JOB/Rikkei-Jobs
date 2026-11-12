import { ROLE_HOME } from "@/config/roles";
import type { Role } from "@/interface/auth";

/**
 * Nhớ trang người dùng định mở khi chưa đăng nhập, để đăng nhập xong quay lại đúng trang.
 * Lưu ở sessionStorage (không phải query) vì luồng SSO đi qua LMS và quay về /sso-callback.
 */
const KEY = "rikkei-jobs-next-path";

export function rememberNextPath(path: string): void {
  try {
    sessionStorage.setItem(KEY, path);
  } catch {
    // Trình duyệt chặn storage: bỏ qua, đăng nhập xong về trang chủ.
  }
}

/** Trang sau đăng nhập: trang đã nhớ nếu thuộc vai trò này, ngược lại là trang chủ của vai trò. Đọc xong thì xoá. */
export function pathAfterLogin(role: Role): string {
  let saved: string | null = null;
  try {
    saved = sessionStorage.getItem(KEY);
    sessionStorage.removeItem(KEY);
  } catch {
    saved = null;
  }
  const home = ROLE_HOME[role];
  return saved && (saved === home || saved.startsWith(`${home}/`)) ? saved : home;
}
