import { ROLE_HOME, ROLE_LANDING } from "@/config/roles";
import type { Role } from "@/interface/auth";

/**
 * Nhớ trang người dùng định mở khi chưa đăng nhập, để đăng nhập xong quay lại đúng trang.
 * Lưu ở sessionStorage (không phải query) vì luồng SSO đi qua LMS và quay về /sso-callback.
 */
const KEY = "rikkei-jobs-next-path";

/** Only canonical, role-owned pathnames; never a URL, query or encoded separator. */
function safeNextPath(path: string): boolean {
  if (!path.startsWith("/") || /[\\?#\s\u0000-\u001f]/.test(path) || /%2f|%5c/i.test(path)) return false;
  try {
    const decoded = decodeURIComponent(path);
    if (/[\\%?#\s\u0000-\u001f]/.test(decoded) || decoded.includes("//")) return false;
    const url = new URL(path, "https://local.invalid");
    return url.origin === "https://local.invalid" && url.pathname === path
      && Object.values(ROLE_HOME).some(home => path === home || path.startsWith(`${home}/`));
  } catch { return false; }
}

export function rememberNextPath(path: string): void {
  if (!safeNextPath(path)) return;
  try {
    sessionStorage.setItem(KEY, path);
  } catch {
    // Trình duyệt chặn storage: bỏ qua, đăng nhập xong về trang chủ.
  }
}

/** Trang sau đăng nhập: trang đã nhớ nếu thuộc vai trò này, ngược lại là ROLE_LANDING (ứng viên về trang chủ). Đọc xong thì xoá. */
export function pathAfterLogin(role: Role): string {
  let saved: string | null = null;
  try {
    saved = sessionStorage.getItem(KEY);
    sessionStorage.removeItem(KEY);
  } catch {
    saved = null;
  }
  const home = ROLE_HOME[role];
  return saved && safeNextPath(saved) && (saved === home || saved.startsWith(`${home}/`)) ? saved : ROLE_LANDING[role];
}
