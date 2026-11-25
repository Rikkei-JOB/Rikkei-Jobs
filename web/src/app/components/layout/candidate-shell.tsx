"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu } from "@base-ui/react/menu";
import { Popover } from "@base-ui/react/popover";
import { ChevronDown, LogOut, Menu as MenuIcon, UserRound, X } from "lucide-react";
import { ROLE_MENU, type MenuItem } from "@/config/menu";
import { useAuthStore } from "@/hooks/use-auth-store";
import type { User } from "@/interface/auth";
import { CandidateNotifications } from "./candidate-notifications";
import "./candidate-shell.css";

const candidateItems = ROLE_MENU.CANDIDATE;
const groups = [
  { label: "Việc làm", items: candidateItems.filter(i => i.href === "/candidate/jobs") },
  { label: "Ứng tuyển", items: candidateItems.filter(i => ["/candidate/applications", "/candidate/interviews", "/candidate/offers"].includes(i.href)) },
  { label: "Hồ sơ", items: candidateItems.filter(i => i.href === "/candidate/profile") },
  { label: "Phát triển", items: candidateItems.filter(i => ["/candidate/roadmap", "/candidate/assistant"].includes(i.href)) },
];
function active(pathname: string, item: MenuItem) { return pathname === item.href || pathname.startsWith(`${item.href}/`); }

export function CandidateShell({ user, children, onLogout }: { user: User; children: ReactNode; onLogout(): void }) {
  const pathname = usePathname();
  const token = useAuthStore(state => state.token);
  const fullName = user.fullName?.trim() || "Ứng viên";
  return <div className="candidate-shell">
    <header className="candidate-header"><div className="candidate-topbar">
      <Link href="/candidate" className="candidate-brand" aria-label="Rikkei Jobs, trang chủ"><span>Rikkei</span> Jobs<span className="candidate-brand-dot" aria-hidden="true" /></Link>
      <nav className="candidate-desktop-nav" aria-label="Menu ứng viên">
        {groups.map(group => group.items.length === 1 ? <Link className="candidate-nav-link" key={group.label} href={group.items[0].href} aria-current={active(pathname, group.items[0]) ? "page" : undefined}>{group.label}</Link>
          : <NavigationGroup key={`${pathname}:${group.label}`} label={group.label} items={group.items} pathname={pathname} />)}
      </nav>
      <div className="candidate-header-actions">
        <CandidateNotifications key={`${user.id}:${token}:${pathname}`} userId={user.id} token={token} />
        <Menu.Root key={`${user.id}:${pathname}`}><Menu.Trigger className="candidate-account" aria-label="Tài khoản ứng viên">
          <span className="candidate-avatar" aria-hidden="true">{fullName.split(/\s+/).slice(-2).map(name => name.charAt(0)).join("")}</span>
          <span className="candidate-account-name">{fullName}</span><ChevronDown aria-hidden="true" /></Menu.Trigger>
          <Menu.Portal><Menu.Positioner side="bottom" align="end" sideOffset={12} collisionPadding={16} className="candidate-overlay"><Menu.Popup className="candidate-popup candidate-account-popup">
            <div className="candidate-account-info"><strong>{fullName}</strong><span>Ứng viên</span></div>
            <Menu.LinkItem render={<Link href="/candidate/profile" />} closeOnClick className="candidate-menu-link"><UserRound aria-hidden="true" />Hồ sơ của tôi</Menu.LinkItem>
            <Menu.Item onClick={onLogout} className="candidate-menu-link"><LogOut aria-hidden="true" />Đăng xuất</Menu.Item>
          </Menu.Popup></Menu.Positioner></Menu.Portal>
        </Menu.Root>
        <MobileNavigation key={pathname} pathname={pathname} />
      </div>
    </div></header>
    <main className="candidate-main">{children}</main>
  </div>;
}

function NavigationGroup({ label, items, pathname }: { label: string; items: MenuItem[]; pathname: string }) {
  return <Menu.Root><Menu.Trigger className="candidate-nav-link" data-active={items.some(item => active(pathname, item)) || undefined} aria-label={`Menu ${label.toLocaleLowerCase("vi-VN")}`}>
    {label}<ChevronDown aria-hidden="true" /></Menu.Trigger>
    <Menu.Portal><Menu.Positioner side="bottom" align="start" sideOffset={8} className="candidate-overlay"><Menu.Popup className="candidate-popup candidate-group-popup">
      {items.map(item => <Menu.LinkItem key={item.href} render={<Link href={item.href} />} closeOnClick className="candidate-menu-link" aria-current={active(pathname, item) ? "page" : undefined}>
        <item.icon aria-hidden="true" />{item.label === "Ứng tuyển" ? "Đơn ứng tuyển" : item.label}</Menu.LinkItem>)}
    </Menu.Popup></Menu.Positioner></Menu.Portal>
  </Menu.Root>;
}

function MobileNavigation({ pathname }: { pathname: string }) {
  return <Popover.Root><Popover.Trigger className="candidate-icon-button candidate-mobile-trigger" aria-label="Mở menu"><MenuIcon aria-hidden="true" /></Popover.Trigger>
    <Popover.Portal><Popover.Positioner side="bottom" align="end" sideOffset={12} collisionPadding={16} className="candidate-overlay"><Popover.Popup className="candidate-popup candidate-mobile-nav">
      <div className="candidate-panel-heading"><Popover.Title>Điều hướng ứng viên</Popover.Title><Popover.Close className="candidate-icon-button" aria-label="Đóng menu"><X aria-hidden="true" /></Popover.Close></div>
      <nav aria-label="Menu ứng viên">{groups.map(group => <div className="candidate-mobile-group" key={group.label}>
        {group.items.length > 1 && <p>{group.label}</p>}
        {group.items.map(item => <Link className="candidate-menu-link" key={item.href} href={item.href} aria-current={active(pathname, item) ? "page" : undefined}><item.icon aria-hidden="true" />{item.label === "Ứng tuyển" ? "Đơn ứng tuyển" : item.label}</Link>)}
      </div>)}</nav>
    </Popover.Popup></Popover.Positioner></Popover.Portal>
  </Popover.Root>;
}
