"use client";

import { useState, type ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu } from "@base-ui/react/menu";
import { Popover } from "@base-ui/react/popover";
import { Building2, ChevronDown, KeyRound, LogOut, Menu as MenuIcon, X } from "lucide-react";
import { ROLE_MENU } from "@/config/menu";
import type { User } from "@/interface/auth";
import { SiteFooter } from "./site-footer";
import { BrandLogo } from "@/app/components/ui/brand-logo";
import "./hr-shell.css";

const items = ROLE_MENU.HR.filter(item => item.href !== "/hr/company");
// Trang tổng quan /hr chỉ sáng khi đúng đường dẫn, không sáng cho mọi trang con.
const selected = (pathname: string, href: string) => pathname === href || (href !== "/hr" && pathname.startsWith(`${href}/`));

export function HrShell({ user, children, onLogout }: { user: User; children: ReactNode; onLogout(): void }) {
  const pathname = usePathname();
  return <div className="hr-shell hr-theme">
    <a className="hr-skip-link" href="#hr-content">Đến nội dung chính</a>
    <header className="hr-header"><div className="hr-topbar">
      <Link href="/hr" className="hr-brand" aria-label="Rikkei Jobs, trang chủ HR"><BrandLogo height={26} /><span className="hr-brand-label">NHÀ TUYỂN DỤNG</span></Link>
      <nav className="hr-desktop-nav" aria-label="Menu HR">{items.map(item => <Link key={item.href} href={item.href} className="hr-nav-link" aria-current={selected(pathname, item.href) ? "page" : undefined}>{item.label}</Link>)}</nav>
      <HeaderActions key={`${user.id}:${pathname}`} user={user} pathname={pathname} onLogout={onLogout} />
    </div></header>
    <main id="hr-content" className="hr-main">{children}</main>
    <SiteFooter compact />
  </div>;
}

function HeaderActions({ user, pathname, onLogout }: { user: User; pathname: string; onLogout(): void }) {
  const [panel, setPanel] = useState<"account" | "navigation" | null>(null);
  const name = user.fullName?.trim() || "Nhà tuyển dụng";
  return <div className="hr-header-actions">
    <Menu.Root open={panel === "account"} onOpenChange={open => setPanel(open ? "account" : null)}>
      <Menu.Trigger className="hr-account" aria-label="Tài khoản HR"><span className="hr-avatar" aria-hidden="true">{name.split(/\s+/).slice(-2).map(part => part[0]).join("")}</span><span className="hr-account-name">{name}</span><ChevronDown size={16} aria-hidden="true" /></Menu.Trigger>
      <Menu.Portal><Menu.Positioner side="bottom" align="end" sideOffset={12} collisionPadding={16} className="hr-overlay hr-theme"><Menu.Popup className="hr-popup hr-account-popup">
        <div className="hr-account-info"><strong>{name}</strong><span>Nhà tuyển dụng · HR</span></div>
        <Menu.LinkItem render={<Link href="/hr/company" />} closeOnClick className="hr-menu-link" aria-current={selected(pathname, "/hr/company") ? "page" : undefined}><Building2 aria-hidden="true" />Thông tin công ty</Menu.LinkItem>
        <Menu.LinkItem render={<Link href="/change-password" />} closeOnClick className="hr-menu-link"><KeyRound aria-hidden="true" />Đổi mật khẩu</Menu.LinkItem>
        <Menu.Item onClick={onLogout} className="hr-menu-link"><LogOut aria-hidden="true" />Đăng xuất</Menu.Item>
      </Menu.Popup></Menu.Positioner></Menu.Portal>
    </Menu.Root>
    <Popover.Root open={panel === "navigation"} onOpenChange={open => setPanel(open ? "navigation" : null)}>
      <Popover.Trigger className="hr-icon-button hr-mobile-trigger" aria-label="Mở menu HR"><MenuIcon aria-hidden="true" /></Popover.Trigger>
      <Popover.Portal><Popover.Positioner side="bottom" align="end" sideOffset={12} collisionPadding={16} className="hr-overlay hr-theme"><Popover.Popup className="hr-popup hr-mobile-nav">
        <div className="hr-panel-heading"><Popover.Title>Không gian tuyển dụng</Popover.Title><Popover.Close className="hr-icon-button" aria-label="Đóng menu HR"><X aria-hidden="true" /></Popover.Close></div>
        <nav aria-label="Menu HR di động">{items.map(item => <Link key={item.href} href={item.href} onClick={() => setPanel(null)} className="hr-menu-link" aria-current={selected(pathname, item.href) ? "page" : undefined}><item.icon aria-hidden="true" />{item.label}</Link>)}</nav>
      </Popover.Popup></Popover.Positioner></Popover.Portal>
    </Popover.Root>
  </div>;
}
