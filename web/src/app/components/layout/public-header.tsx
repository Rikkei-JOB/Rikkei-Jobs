"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu } from "@base-ui/react/menu";
import { Popover } from "@base-ui/react/popover";
import { ArrowRight, BookOpen, Briefcase, Building2, ChevronDown, FileText, GraduationCap, LogOut, Menu as MenuIcon, UserRound, X, type LucideIcon } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { BrandLogo } from "@/app/components/ui/brand-logo";
import { ROLE_MENU } from "@/config/menu";
import { ROLE_HOME, ROLE_LABEL } from "@/config/roles";
import { useAuthStore } from "@/hooks/use-auth-store";
import { useLogout } from "@/hooks/use-logout";
import type { User } from "@/interface/auth";
import { CandidateNotifications } from "./candidate-notifications";
import "./candidate-shell.css";

interface NavLink { href: string; label: string; icon: LucideIcon }
interface NavGroup { label: string; items: NavLink[] }

const JOBS: NavLink = { href: "/jobs", label: "Việc làm IT", icon: Briefcase };
const COMPANIES: NavLink = { href: "/companies", label: "Công ty", icon: Building2 };

/** Lấy mục của ứng viên từ ROLE_MENU (một nguồn cho nhãn và biểu tượng); "Ứng tuyển" hiển thị là "Đơn ứng tuyển". */
function candidateItems(hrefs: string[]): NavLink[] {
  return ROLE_MENU.CANDIDATE.filter(item => hrefs.includes(item.href))
    .map(item => ({ ...item, label: item.label === "Ứng tuyển" ? "Đơn ứng tuyển" : item.label }));
}

/** Điều hướng của ứng viên đã đăng nhập: giống nhau ở trang chủ, trang việc làm và mọi trang /candidate. */
const CANDIDATE_NAV: (NavLink | NavGroup)[] = [
  JOBS,
  COMPANIES,
  { label: "Ứng tuyển", items: candidateItems(["/candidate/applications", "/candidate/interviews"]) },
  // UT-03: lịch sử doanh nghiệp xem thông tin liên hệ nằm cùng nhóm hồ sơ (như khung ứng viên cũ).
  { label: "Hồ sơ", items: candidateItems(["/candidate/profile", "/candidate/cvs", "/candidate/contact-access-logs"]) },
  { label: "Phát triển", items: candidateItems(["/candidate/roadmap", "/candidate/assistant"]) },
];

const isGroup = (entry: NavLink | NavGroup): entry is NavGroup => "items" in entry;
const isActive = (pathname: string, href: string) => pathname === href || pathname.startsWith(`${href}/`);
const linkClass = (active: boolean) =>
  `inline-flex h-16 items-center gap-1 whitespace-nowrap border-b-2 transition-colors ${active ? "border-[#b4232d] text-[#b4232d]" : "border-transparent hover:text-[#b4232d]"}`;

/**
 * Header dùng chung cho trang công khai (trang chủ, việc làm, công ty) và khung ứng viên (CandidateShell),
 * để ứng viên đi giữa các trang không thấy header đổi kiểu.
 */
export function PublicHeader() {
  const pathname = usePathname();
  const user = useAuthStore(s => s.user);
  const candidate = user?.role === "CANDIDATE" ? user : null;

  return <header className="sticky top-0 z-30 border-b border-border bg-white shadow-[0_2px_10px_rgb(15_23_42/0.03)]">
    <div className="mx-auto flex h-16 max-w-[1440px] items-center justify-between gap-4 px-4 sm:px-8">
      <Link href="/" className="flex shrink-0 flex-col justify-center gap-1 leading-tight" aria-label="Rikkei Jobs, trang chủ">
        <BrandLogo height={28} />
        <span className="block text-[10px] font-bold tracking-[0.1em] text-muted-foreground uppercase max-sm:hidden">Đổi mới Sáng tạo &amp; Sự nghiệp</span>
      </Link>

      <nav aria-label={candidate ? "Menu ứng viên" : "Điều hướng chính"} className="hidden items-center gap-6 self-stretch text-sm font-semibold text-heading lg:flex xl:gap-7">
        {candidate
          ? CANDIDATE_NAV.map(entry => isGroup(entry)
            ? <NavDropdown key={`${pathname}:${entry.label}`} group={entry} pathname={pathname} />
            : <Link key={entry.href} href={entry.href} className={linkClass(isActive(pathname, entry.href))} aria-current={isActive(pathname, entry.href) ? "page" : undefined}>{entry.label}</Link>)
          : <GuestLinks pathname={pathname} />}
      </nav>

      <div className="flex shrink-0 items-center gap-2">
        {candidate ? <CandidateActions key={pathname} user={candidate} pathname={pathname} />
          : user ? <Button size="lg" render={<Link href={ROLE_HOME[user.role]} />} nativeButton={false}>
            <span className="max-sm:hidden">Vào không gian {ROLE_LABEL[user.role]}</span><span className="sm:hidden">{ROLE_LABEL[user.role]}</span>
            <ArrowRight aria-hidden="true" /></Button>
          : <>
            <Link href="/login?tab=hr" className="ptit-role-pill ptit-role-pill-hr max-sm:hidden"><Building2 className="size-3.5" aria-hidden="true" /><span>Nhà tuyển dụng</span></Link>
            <Link href="/login" className="ptit-role-pill ptit-role-pill-staff max-md:hidden"><BookOpen className="size-3.5" aria-hidden="true" /><span>CB / Mentor</span></Link>
            <Link href="/login" className="ptit-role-pill ptit-role-pill-candidate"><GraduationCap className="size-3.5" aria-hidden="true" /><span>Đăng nhập Ứng viên</span></Link>
          </>}
      </div>
    </div>
    {/* Khách và vai trò khác: hàng liên kết thứ hai trên màn hẹp. Ứng viên dùng nút menu ☰ ở góc phải. */}
    {!candidate && <nav aria-label="Điều hướng chính di động" className="flex h-11 items-center gap-6 overflow-x-auto border-t border-border px-4 text-sm font-semibold text-heading sm:px-8 lg:hidden">
      <GuestLinks pathname={pathname} />
    </nav>}
  </header>;
}

function GuestLinks({ pathname }: { pathname: string }) {
  return <>
    {[JOBS, COMPANIES].map(item => <Link key={item.href} href={item.href} className={`whitespace-nowrap transition-colors ${isActive(pathname, item.href) ? "text-[#b4232d]" : "hover:text-[#b4232d]"}`} aria-current={isActive(pathname, item.href) ? "page" : undefined}>{item.label}</Link>)}
    <Link href="/login" className="whitespace-nowrap transition-colors hover:text-[#b4232d]">Hồ sơ năng lực</Link>
  </>;
}

function NavDropdown({ group, pathname }: { group: NavGroup; pathname: string }) {
  const active = group.items.some(item => isActive(pathname, item.href));
  return <Menu.Root>
    <Menu.Trigger className={linkClass(active)} aria-label={`Menu ${group.label.toLocaleLowerCase("vi-VN")}`}>{group.label}<ChevronDown className="size-3.5" aria-hidden="true" /></Menu.Trigger>
    <Menu.Portal><Menu.Positioner side="bottom" align="start" sideOffset={4} className="candidate-overlay"><Menu.Popup className="candidate-popup candidate-group-popup">
      {group.items.map(item => <Menu.LinkItem key={item.href} render={<Link href={item.href} />} closeOnClick className="candidate-menu-link" aria-current={isActive(pathname, item.href) ? "page" : undefined}>
        <item.icon aria-hidden="true" />{item.label}</Menu.LinkItem>)}
    </Menu.Popup></Menu.Positioner></Menu.Portal>
  </Menu.Root>;
}

/** Chuông thông báo, menu tài khoản và (màn hẹp) nút mở toàn bộ điều hướng ứng viên. */
function CandidateActions({ user, pathname }: { user: User; pathname: string }) {
  const token = useAuthStore(s => s.token);
  const logout = useLogout();
  const fullName = user.fullName?.trim() || "Ứng viên";
  return <div className="flex items-center gap-1">
    <CandidateNotifications key={`${user.id}:${token}`} userId={user.id} token={token} />
    <Menu.Root><Menu.Trigger className="candidate-account" aria-label="Tài khoản ứng viên">
      <span className="candidate-avatar" aria-hidden="true">{fullName.split(/\s+/).slice(-2).map(name => name.charAt(0)).join("")}</span>
      <span className="candidate-account-name max-xl:hidden">{fullName}</span><ChevronDown className="max-md:hidden" aria-hidden="true" /></Menu.Trigger>
      <Menu.Portal><Menu.Positioner side="bottom" align="end" sideOffset={12} collisionPadding={16} className="candidate-overlay"><Menu.Popup className="candidate-popup candidate-account-popup">
        <div className="candidate-account-info"><strong>{fullName}</strong><span>Ứng viên</span></div>
        <Menu.LinkItem render={<Link href="/candidate/profile" />} closeOnClick className="candidate-menu-link"><UserRound aria-hidden="true" />Hồ sơ của tôi</Menu.LinkItem>
        <Menu.LinkItem render={<Link href="/candidate/cvs" />} closeOnClick className="candidate-menu-link"><FileText aria-hidden="true" />CV của tôi</Menu.LinkItem>
        <Menu.Item onClick={logout} className="candidate-menu-link"><LogOut aria-hidden="true" />Đăng xuất</Menu.Item>
      </Menu.Popup></Menu.Positioner></Menu.Portal>
    </Menu.Root>
    {/* Bọc trong span vì .candidate-icon-button (CSS ngoài layer) đè lên lg:hidden của Tailwind. */}
    <span className="lg:hidden"><Popover.Root><Popover.Trigger className="candidate-icon-button" aria-label="Mở menu"><MenuIcon aria-hidden="true" /></Popover.Trigger>
      <Popover.Portal><Popover.Positioner side="bottom" align="end" sideOffset={12} collisionPadding={16} className="candidate-overlay"><Popover.Popup className="candidate-popup candidate-mobile-nav">
        <div className="candidate-panel-heading"><Popover.Title>Điều hướng ứng viên</Popover.Title><Popover.Close className="candidate-icon-button" aria-label="Đóng menu"><X aria-hidden="true" /></Popover.Close></div>
        <nav aria-label="Menu ứng viên">{CANDIDATE_NAV.map(entry => {
          const items = isGroup(entry) ? entry.items : [entry];
          return <div className="candidate-mobile-group" key={isGroup(entry) ? entry.label : entry.href}>
            {isGroup(entry) && <p>{entry.label}</p>}
            {items.map(item => <Link className="candidate-menu-link" key={item.href} href={item.href} aria-current={isActive(pathname, item.href) ? "page" : undefined}><item.icon aria-hidden="true" />{item.label}</Link>)}
          </div>;
        })}</nav>
      </Popover.Popup></Popover.Positioner></Popover.Portal>
    </Popover.Root></span>
  </div>;
}
