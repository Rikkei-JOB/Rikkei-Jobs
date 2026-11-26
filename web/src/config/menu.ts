import {
  BarChart3,
  Bot,
  Briefcase,
  Building2,
  ClipboardCheck,
  CalendarDays,
  Contact,
  FileCheck2,
  FileText,
  GraduationCap,
  History,
  LayoutDashboard,
  Handshake,
  Map,
  RefreshCw,
  Send,
  Sparkles,
  UserRound,
  UserSearch,
  Users,
  type LucideIcon,
} from "lucide-react";
import type { Role } from "@/interface/auth";

export interface MenuItem {
  label: string;
  /** Luôn bắt đầu bằng trang chủ của vai trò (ROLE_HOME), ví dụ /hr/jobs. */
  href: string;
  icon: LucideIcon;
}

/**
 * Menu theo vai trò (NT-02). Task tính năng thêm trang thật ở đúng `href` (ví dụ `app/(pages)/(app)/hr/jobs/page.tsx`);
 * khi chưa có, trang tạm "Đang phát triển" hiển thị thay.
 */
export const ROLE_MENU: Record<Role, MenuItem[]> = {
  CANDIDATE: [
    { label: "Lịch sử xem liên hệ", href: "/candidate/contact-access-logs", icon: Users },
    { label: "Hồ sơ", href: "/candidate/profile", icon: UserRound },
    { label: "CV của tôi", href: "/candidate/cvs", icon: FileText },
    { label: "Việc làm", href: "/candidate/jobs", icon: Briefcase },
    { label: "Ứng tuyển", href: "/candidate/applications", icon: Send },
    { label: "Phỏng vấn", href: "/candidate/interviews", icon: CalendarDays },
    { label: "Lộ trình", href: "/candidate/roadmap", icon: Map },
    { label: "Trợ lý AI", href: "/candidate/assistant", icon: Bot },
  ],
  HR: [
    { label: "Tổng quan", href: "/hr", icon: LayoutDashboard },
    { label: "Công ty", href: "/hr/company", icon: Building2 },
    { label: "Tin tuyển dụng", href: "/hr/jobs", icon: FileText },
    { label: "Hồ sơ ứng tuyển", href: "/hr/recruitment", icon: UserSearch },
    { label: "Lịch phỏng vấn", href: "/hr/interviews", icon: CalendarDays },
    { label: "Thực tập sinh", href: "/hr/interns", icon: GraduationCap },
  ],
  MENTOR: [
    { label: "Tổng quan", href: "/mentor", icon: LayoutDashboard },
    { label: "Lịch phỏng vấn", href: "/mentor/schedule", icon: CalendarDays },
    { label: "Đánh giá", href: "/mentor/reviews", icon: ClipboardCheck },
  ],
  STAFF: [
    { label: "Tổng quan", href: "/staff", icon: LayoutDashboard },
    { label: "Duyệt hồ sơ", href: "/staff/profile-reviews", icon: Users },
    { label: "Duyệt JD", href: "/staff/jobs", icon: FileCheck2 },
    // UT-03: icon riêng để không trùng "Duyệt hồ sơ" trên rail của cán bộ.
    { label: "Mở liên hệ", href: "/staff/contact-requests", icon: Contact },
    { label: "Nhật ký liên hệ", href: "/staff/contact-access-logs", icon: History },
    { label: "Đơn ứng tuyển", href: "/staff/applications", icon: UserSearch },
    { label: "Doanh nghiệp", href: "/staff/companies", icon: Building2 },
    { label: "Ghép thực tập", href: "/staff/internships", icon: Handshake },
    { label: "Đồng bộ dữ liệu", href: "/staff/sync", icon: RefreshCw },
    { label: "Báo cáo", href: "/staff/reports", icon: BarChart3 },
  ],
  ADMIN: [
    { label: "Tổng quan", href: "/admin", icon: LayoutDashboard },
    { label: "Tài khoản", href: "/admin/users", icon: Users },
    { label: "Duyệt JD", href: "/admin/jobs", icon: FileCheck2 },
    { label: "Đơn ứng tuyển", href: "/admin/applications", icon: UserSearch },
    { label: "Kỹ năng", href: "/admin/skills", icon: Sparkles },
  ],
};

/** Mục menu đang mở (khớp đúng hoặc là trang con của nó). */
export function findMenuItem(role: Role, pathname: string): MenuItem | undefined {
  return ROLE_MENU[role].find((item) => pathname === item.href || pathname.startsWith(`${item.href}/`));
}

export interface NavGroup {
  label: string;
  items: MenuItem[];
}

const pick = (role: Role, ...hrefs: string[]): MenuItem[] =>
  hrefs.map((href) => ROLE_MENU[role].find((item) => item.href === href)!);

/** Nhóm menu: cán bộ (rail icon, ngăn cách theo nhóm) và quản trị (sidebar có tiêu đề nhóm). */
export const ROLE_NAV_GROUPS: Partial<Record<Role, NavGroup[]>> = {
  STAFF: [
    { label: "Tổng quan", items: pick("STAFF", "/staff") },
    { label: "Duyệt", items: pick("STAFF", "/staff/profile-reviews", "/staff/jobs", "/staff/contact-requests") },
    ...pick("STAFF", "/staff/applications", "/staff/companies", "/staff/internships").map((item) => ({
      label: item.label,
      items: [item],
    })),
    { label: "Hệ thống", items: pick("STAFF", "/staff/sync", "/staff/reports", "/staff/contact-access-logs") },
  ],
  // Nhóm mục ở sidebar quản trị (AdminShell).
  ADMIN: [
    { label: "Tổng quan", items: pick("ADMIN", "/admin") },
    { label: "Người dùng", items: pick("ADMIN", "/admin/users") },
    { label: "Kiểm duyệt", items: pick("ADMIN", "/admin/jobs", "/admin/applications") },
    { label: "Danh mục", items: pick("ADMIN", "/admin/skills") },
  ],
};
