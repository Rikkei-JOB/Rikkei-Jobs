"use client";

import { useEffect, type ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu } from "@base-ui/react/menu";
import { Popover } from "@base-ui/react/popover";
import { ChevronDown, KeyRound, LogOut, Menu as MenuIcon, X } from "lucide-react";
import { ROLE_NAV_GROUPS, type MenuItem } from "@/config/menu";
import { ROLE_HOME, ROLE_LABEL } from "@/config/roles";
import type { User } from "@/interface/auth";
import "./manager-shell.css";

function active(pathname: string, item: MenuItem) { return pathname === item.href || pathname.startsWith(`${item.href}/`); }

/** Khung thanh ngang cho HR và cán bộ (cùng kiểu thanh của ứng viên, không có chuông thông báo). */
export function ManagerShell({ user, children, onLogout }: { user: User; children: ReactNode; onLogout(): void }) {
  const pathname = usePathname();
  const groups = ROLE_NAV_GROUPS[user.role] ?? [];
  const roleLabel = ROLE_LABEL[user.role];
  const fullName = user.fullName?.trim() || roleLabel;
  // Dialog/Select/Popover của trang được portal ra body, ngoài khung này; gắn theme lên body để chúng cũng đỏ.
  useEffect(() => {
    document.body.classList.add("admin-theme");
    return () => document.body.classList.remove("admin-theme");
  }, []);
  return <div className="admin-theme manager-shell">
    <header className="manager-header"><div className="manager-topbar">
      <Link href={ROLE_HOME[user.role]} className="manager-brand" aria-label="Rikkei Jobs, trang chủ"><span>Rikkei</span> Jobs<span className="manager-brand-dot" aria-hidden="true" /></Link>
      <nav className="manager-desktop-nav" aria-label={`Menu ${roleLabel}`}>
        {groups.map(group => group.items.length === 1 ? <Link className="manager-nav-link" key={group.label} href={group.items[0].href} aria-current={active(pathname, group.items[0]) ? "page" : undefined}>{group.label}</Link>
          : <NavigationGroup key={`${pathname}:${group.label}`} label={group.label} items={group.items} pathname={pathname} />)}
      </nav>
      <div className="manager-header-actions">
        <Menu.Root key={`${user.id}:${pathname}`}><Menu.Trigger className="manager-account" aria-label={`Tài khoản ${roleLabel}`}>
          <span className="manager-avatar" aria-hidden="true">{fullName.split(/\s+/).slice(-2).map(name => name.charAt(0)).join("")}</span>
          <span className="manager-account-name">{fullName}</span><ChevronDown aria-hidden="true" /></Menu.Trigger>
          <Menu.Portal><Menu.Positioner side="bottom" align="end" sideOffset={12} collisionPadding={16} className="manager-overlay"><Menu.Popup className="manager-popup manager-account-popup">
            <div className="manager-account-info"><strong>{fullName}</strong><span>{roleLabel}</span></div>
            {user.role === "HR" && <Menu.LinkItem render={<Link href="/change-password" />} closeOnClick className="manager-menu-link"><KeyRound aria-hidden="true" />Đổi mật khẩu</Menu.LinkItem>}
            <Menu.Item onClick={onLogout} className="manager-menu-link"><LogOut aria-hidden="true" />Đăng xuất</Menu.Item>
          </Menu.Popup></Menu.Positioner></Menu.Portal>
        </Menu.Root>
        <MobileNavigation key={pathname} pathname={pathname} title={`Điều hướng ${roleLabel}`} groups={groups} />
      </div>
    </div></header>
    <main className="manager-main">{children}</main>
  </div>;
}

function NavigationGroup({ label, items, pathname }: { label: string; items: MenuItem[]; pathname: string }) {
  return <Menu.Root><Menu.Trigger className="manager-nav-link" data-active={items.some(item => active(pathname, item)) || undefined} aria-label={`Menu ${label.toLocaleLowerCase("vi-VN")}`}>
    {label}<ChevronDown aria-hidden="true" /></Menu.Trigger>
    <Menu.Portal><Menu.Positioner side="bottom" align="start" sideOffset={8} className="manager-overlay"><Menu.Popup className="manager-popup manager-group-popup">
      {items.map(item => <Menu.LinkItem key={item.href} render={<Link href={item.href} />} closeOnClick className="manager-menu-link" aria-current={active(pathname, item) ? "page" : undefined}>
        <item.icon aria-hidden="true" />{item.label}</Menu.LinkItem>)}
    </Menu.Popup></Menu.Positioner></Menu.Portal>
  </Menu.Root>;
}

function MobileNavigation({ pathname, title, groups }: { pathname: string; title: string; groups: { label: string; items: MenuItem[] }[] }) {
  return <Popover.Root><Popover.Trigger className="manager-icon-button manager-mobile-trigger" aria-label="Mở menu"><MenuIcon aria-hidden="true" /></Popover.Trigger>
    <Popover.Portal><Popover.Positioner side="bottom" align="end" sideOffset={12} collisionPadding={16} className="manager-overlay"><Popover.Popup className="manager-popup manager-mobile-nav">
      <div className="manager-panel-heading"><Popover.Title>{title}</Popover.Title><Popover.Close className="manager-icon-button" aria-label="Đóng menu"><X aria-hidden="true" /></Popover.Close></div>
      <nav aria-label={title}>{groups.map(group => <div className="manager-mobile-group" key={group.label}>
        {group.items.length > 1 && <p>{group.label}</p>}
        {group.items.map(item => <Link className="manager-menu-link" key={item.href} href={item.href} aria-current={active(pathname, item) ? "page" : undefined}><item.icon aria-hidden="true" />{item.label}</Link>)}
      </div>)}</nav>
    </Popover.Popup></Popover.Positioner></Popover.Portal>
  </Popover.Root>;
}
