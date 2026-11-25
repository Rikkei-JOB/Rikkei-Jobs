import {
  BarChart3,
  Bot,
  Briefcase,
  Building2,
  ClipboardCheck,
  CalendarDays,
  FileCheck2,
  FileText,
  GraduationCap,
  Handshake,
  Map,
  RefreshCw,
  Send,
  Settings,
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
    { label: "Hồ sơ", href: "/candidate/profile", icon: UserRound },
    { label: "Việc làm", href: "/candidate/jobs", icon: Briefcase },
    { label: "Ứng tuyển", href: "/candidate/applications", icon: Send },
    { label: "Offer", href: "/candidate/offers", icon: Handshake },
    { label: "Phỏng vấn", href: "/candidate/interviews", icon: CalendarDays },
    { label: "Lộ trình", href: "/candidate/roadmap", icon: Map },
    { label: "Trợ lý AI", href: "/candidate/assistant", icon: Bot },
  ],
  HR: [
    { label: "Công ty", href: "/hr/company", icon: Building2 },
    { label: "Tin tuyển dụng", href: "/hr/jobs", icon: FileText },
    { label: "Hồ sơ ứng tuyển", href: "/hr/recruitment", icon: UserSearch },
    { label: "Lịch phỏng vấn", href: "/hr/interviews", icon: CalendarDays },
    { label: "Thực tập sinh", href: "/hr/interns", icon: GraduationCap },
  ],
  MENTOR: [{ label: "Cần đánh giá", href: "/mentor/reviews", icon: ClipboardCheck }],
  STAFF: [
    { label: "Duyệt hồ sơ", href: "/staff/profile-reviews", icon: Users },
    { label: "Duyệt JD", href: "/staff/jobs", icon: FileCheck2 },
    { label: "Đơn ứng tuyển", href: "/staff/applications", icon: UserSearch },
    { label: "Doanh nghiệp", href: "/staff/companies", icon: Building2 },
    { label: "Ghép thực tập", href: "/staff/internships", icon: Handshake },
    { label: "Đồng bộ dữ liệu", href: "/staff/sync", icon: RefreshCw },
    { label: "Báo cáo", href: "/staff/reports", icon: BarChart3 },
  ],
  ADMIN: [
    { label: "Quản trị", href: "/admin/settings", icon: Settings },
    { label: "Đơn ứng tuyển", href: "/admin/applications", icon: UserSearch },
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

/** Nhóm menu trên thanh ngang (HR, cán bộ): nhóm 1 mục là link phẳng, nhóm từ 2 mục là menu thả xuống. */
export const ROLE_NAV_GROUPS: Partial<Record<Role, NavGroup[]>> = {
  HR: ROLE_MENU.HR.map((item) => ({ label: item.label, items: [item] })),
  STAFF: [
    { label: "Duyệt", items: pick("STAFF", "/staff/profile-reviews", "/staff/jobs") },
    ...pick("STAFF", "/staff/applications", "/staff/companies", "/staff/internships").map((item) => ({
      label: item.label,
      items: [item],
    })),
    { label: "Hệ thống", items: pick("STAFF", "/staff/sync", "/staff/reports") },
  ],
};
