"use client";

import { useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu } from "@base-ui/react/menu";
import { Popover } from "@base-ui/react/popover";
import { ChevronRight, ChevronsUpDown, LogOut, Menu as MenuIcon, UserRound, X } from "lucide-react";
import { ROLE_MENU, ROLE_NAV_GROUPS, type MenuItem, type NavGroup } from "@/config/menu";
import { ROLE_HOME, ROLE_LABEL } from "@/config/roles";
import type { Role, User } from "@/interface/auth";
import { BrandLogo } from "@/app/components/ui/brand-logo";
import { SiteFooter } from "./site-footer";
import "./hr-shell.css";
import "./manager-shell.css";
import "./role-shells.css";

/**
 * Khung riêng cho từng vai trò làm việc, cùng nền sáng và màu thương hiệu nhưng khác bố cục:
 * HR giữ thanh ngang (HrShell); Mentor là sidebar trắng; Cán bộ là thanh trên + rail icon;
 * Quản trị là sidebar nhóm mục + thanh trên có breadcrumb.
 * Nội dung trang vẫn nằm trong `.hr-main` (Mentor, Quản trị) hoặc `.manager-main` (Cán bộ) như trước.
 */
type ShellProps = { user: User; children: ReactNode; onLogout(): void };
type ShellRole = Extract<Role, "MENTOR" | "STAFF" | "ADMIN">;

const BRAND_LABEL: Record<ShellRole, string> = { MENTOR: "MENTOR", STAFF: "CÁN BỘ TUYỂN DỤNG", ADMIN: "QUẢN TRỊ HỆ THỐNG" };
const NAV_TITLE: Record<ShellRole, string> = { MENTOR: "Không gian đánh giá", STAFF: "Điều hướng cán bộ", ADMIN: "Không gian quản trị" };

/** Trang chủ vai trò chỉ sáng khi đúng đường dẫn; mục khác sáng cả ở trang con. */
const isActive = (pathname: string, href: string) =>
  pathname === href || (!Object.values(ROLE_HOME).includes(href) && pathname.startsWith(`${href}/`));
const initials = (name: string) => name.split(/\s+/).slice(-2).map(part => part.charAt(0)).join("").toUpperCase();
const groupsOf = (role: ShellRole): NavGroup[] => ROLE_NAV_GROUPS[role] ?? ROLE_MENU[role].map(item => ({ label: item.label, items: [item] }));

function Brand({ role, compact = false }: { role: ShellRole; compact?: boolean }) {
  return <Link href={ROLE_HOME[role]} className="rs-brand" aria-label={`Rikkei Jobs, trang chủ ${ROLE_LABEL[role]}`}>
    <BrandLogo height={compact ? 24 : 26} /><span className="rs-brand-label">{BRAND_LABEL[role]}</span>
  </Link>;
}

/** Menu tài khoản: "card" ở đáy sidebar (Mentor), "compact" trên thanh trên. */
function AccountMenu({ user, role, onLogout, variant }: { user: User; role: ShellRole; onLogout(): void; variant: "card" | "compact" }) {
  const pathname = usePathname();
  const name = user.fullName?.trim() || ROLE_LABEL[role];
  return <Menu.Root key={`${user.id}:${pathname}`}>
    <Menu.Trigger className={variant === "card" ? "rs-account-card" : "rs-account"} aria-label={`Tài khoản ${name}`}>
      <span className="rs-avatar" aria-hidden="true">{initials(name)}</span>
      <span className="rs-account-text"><strong>{name}</strong>{variant === "card" && <small>{ROLE_LABEL[role]}</small>}</span>
      <ChevronsUpDown className="rs-account-chevron" aria-hidden="true" />
    </Menu.Trigger>
    <Menu.Portal><Menu.Positioner side={variant === "card" ? "top" : "bottom"} align={variant === "card" ? "start" : "end"} sideOffset={10} collisionPadding={16} className="hr-overlay hr-theme">
      <Menu.Popup className="hr-popup hr-account-popup">
        <div className="hr-account-info"><strong>{name}</strong><span>{ROLE_LABEL[role]}{user.email ? ` · ${user.email}` : ""}</span></div>
        {/* Các vai trò này đăng nhập qua LMS nên không có mục đổi mật khẩu như HR. */}
        <Menu.LinkItem render={<Link href={`${ROLE_HOME[role]}/profile`} />} closeOnClick className="hr-menu-link"><UserRound aria-hidden="true" />Hồ sơ cá nhân</Menu.LinkItem>
        <Menu.Item onClick={onLogout} className="hr-menu-link"><LogOut aria-hidden="true" />Đăng xuất</Menu.Item>
      </Menu.Popup>
    </Menu.Positioner></Menu.Portal>
  </Menu.Root>;
}

/** Menu thả xuống cho màn hình dưới 1024px, khi sidebar/rail ẩn. */
function MobileNav({ role }: { role: ShellRole }) {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const groups = groupsOf(role);
  return <Popover.Root key={pathname} open={open} onOpenChange={setOpen}>
    <Popover.Trigger className="hr-icon-button rs-mobile-trigger" aria-label="Mở menu"><MenuIcon aria-hidden="true" /></Popover.Trigger>
    <Popover.Portal><Popover.Positioner side="bottom" align="start" sideOffset={12} collisionPadding={16} className="hr-overlay hr-theme"><Popover.Popup className="hr-popup hr-mobile-nav">
      <div className="hr-panel-heading"><Popover.Title>{NAV_TITLE[role]}</Popover.Title><Popover.Close className="hr-icon-button" aria-label="Đóng menu"><X aria-hidden="true" /></Popover.Close></div>
      <nav aria-label={`${NAV_TITLE[role]} di động`}>{groups.map(group => <div key={group.label}>
        {group.items.length > 1 && <p className="rs-mobile-group">{group.label}</p>}
        {group.items.map(item => <Link key={item.href} href={item.href} onClick={() => setOpen(false)} className="hr-menu-link" aria-current={isActive(pathname, item.href) ? "page" : undefined}><item.icon aria-hidden="true" />{item.label}</Link>)}
      </div>)}</nav>
    </Popover.Popup></Popover.Positioner></Popover.Portal>
  </Popover.Root>;
}

function NavLink({ item, pathname, className }: { item: MenuItem; pathname: string; className: string }) {
  return <Link href={item.href} className={className} aria-current={isActive(pathname, item.href) ? "page" : undefined}>
    <item.icon aria-hidden="true" /><span>{item.label}</span>
  </Link>;
}

/** Mentor: sidebar trắng cố định bên trái, tài khoản ở đáy; không có thanh trên trên desktop. */
export function MentorShell({ user, children, onLogout }: ShellProps) {
  const pathname = usePathname();
  return <div className="hr-shell hr-theme rs rs-sidebar">
    <a className="hr-skip-link" href="#mentor-content">Đến nội dung chính</a>
    <aside className="rs-side" aria-label="Thanh bên Mentor">
      <Brand role="MENTOR" />
      <p className="rs-side-caption">{NAV_TITLE.MENTOR}</p>
      <nav className="rs-side-nav" aria-label="Menu Mentor">{ROLE_MENU.MENTOR.map(item => <NavLink key={item.href} item={item} pathname={pathname} className="rs-side-link" />)}</nav>
      <div className="rs-side-foot"><AccountMenu user={user} role="MENTOR" onLogout={onLogout} variant="card" /></div>
    </aside>
    <div className="rs-body">
      <header className="rs-mobilebar"><MobileNav role="MENTOR" /><Brand role="MENTOR" compact /><div className="rs-spacer" /><AccountMenu user={user} role="MENTOR" onLogout={onLogout} variant="compact" /></header>
      <main id="mentor-content" className="hr-main">{children}</main>
      <SiteFooter compact />
    </div>
  </div>;
}

/** Quản trị: sidebar xám nhạt chia nhóm mục + thanh trên có breadcrumb và tài khoản. */
export function AdminShell({ user, children, onLogout }: ShellProps) {
  const pathname = usePathname();
  const groups = groupsOf("ADMIN");
  const current = pathname === "/admin/profile" ? "Hồ sơ cá nhân" : ROLE_MENU.ADMIN.find(item => isActive(pathname, item.href))?.label;
  return <div className="hr-shell hr-theme rs rs-console">
    <a className="hr-skip-link" href="#admin-content">Đến nội dung chính</a>
    <aside className="rs-side" aria-label="Thanh bên quản trị">
      <Brand role="ADMIN" />
      <nav className="rs-side-nav" aria-label="Menu Quản trị">{groups.map(group => <div key={group.label} className="rs-side-group">
        {/* Nhóm một mục trùng tên (Tổng quan) không cần tiêu đề. */}
        {!(group.items.length === 1 && group.items[0].label === group.label) && <p className="rs-side-caption">{group.label}</p>}
        {group.items.map(item => <NavLink key={item.href} item={item} pathname={pathname} className="rs-side-link" />)}
      </div>)}</nav>
    </aside>
    <div className="rs-body">
      <header className="rs-topbar">
        <MobileNav role="ADMIN" />
        <div className="rs-topbar-brand"><Brand role="ADMIN" compact /></div>
        <nav className="rs-crumbs" aria-label="Vị trí hiện tại"><Link href="/admin">Quản trị</Link>{current && current !== "Tổng quan" && <><ChevronRight aria-hidden="true" /><span aria-current="page">{current}</span></>}</nav>
        <div className="rs-spacer" />
        <AccountMenu user={user} role="ADMIN" onLogout={onLogout} variant="compact" />
      </header>
      <main id="admin-content" className="hr-main">{children}</main>
      <SiteFooter compact />
    </div>
  </div>;
}

/** Cán bộ: thanh trên toàn chiều ngang + rail icon hẹp (nhãn dưới biểu tượng, ngăn cách theo nhóm). */
export function StaffShell({ user, children, onLogout }: ShellProps) {
  const pathname = usePathname();
  const groups = groupsOf("STAFF");
  // Dialog/Select/Popover của trang được portal ra body, ngoài khung này; gắn theme lên body để chúng cũng đỏ.
  useEffect(() => {
    document.body.classList.add("admin-theme");
    return () => document.body.classList.remove("admin-theme");
  }, []);
  return <div className="admin-theme rs rs-rail">
    <a className="hr-skip-link" href="#staff-content">Đến nội dung chính</a>
    <header className="rs-railbar">
      <MobileNav role="STAFF" />
      <Brand role="STAFF" />
      <div className="rs-spacer" />
      <AccountMenu user={user} role="STAFF" onLogout={onLogout} variant="compact" />
    </header>
    <div className="rs-rail-body">
      <nav className="rs-rail-nav" aria-label="Menu cán bộ">{groups.map(group => <div key={group.label} className="rs-rail-group" role="group" aria-label={group.label}>
        {group.items.map(item => <NavLink key={item.href} item={item} pathname={pathname} className="rs-rail-link" />)}
      </div>)}</nav>
      <div className="rs-body">
        <main id="staff-content" className="manager-main">{children}</main>
        <SiteFooter compact />
      </div>
    </div>
  </div>;
}
