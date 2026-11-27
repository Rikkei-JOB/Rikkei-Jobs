"use client";

import { Fragment, Suspense, useEffect, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { AlertCircle, CheckCircle2, Lock, LockOpen, Search, Users } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { SelectMenu } from "@/app/components/ui/select-menu";
import { TaskBanner } from "@/app/components/ui/task-banner";
import { ROLE_LABEL } from "@/config/roles";
import { useAdminUserSummary, useAdminUsers, useToggleUserLock } from "@/hooks/use-admin";
import { useAuthStore } from "@/hooks/use-auth-store";
import type { Role } from "@/interface/auth";
import type { AdminUser } from "@/services/admin.service";
import { getErrorMessage } from "@/services/api";
import "./admin.css";

const PAGE_SIZE = 20;
const ROLES: Role[] = ["CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN"];
/** Màu nhãn vai trò, cùng tông với thẻ vai trò ở trang Tổng quan. */
export const ROLE_TONE: Record<Role, { bg: string; fg: string; tone: string }> = {
  CANDIDATE: { bg: "#fff1f2", fg: "#b4232d", tone: "brand" },
  HR: { bg: "#eff6ff", fg: "#1d4ed8", tone: "blue" },
  MENTOR: { bg: "#f5f3ff", fg: "#6d28d9", tone: "violet" },
  STAFF: { bg: "#fffbeb", fg: "#b45309", tone: "amber" },
  ADMIN: { bg: "#f1f5f9", fg: "#334155", tone: "slate" },
};
const QUICK_REASONS = ["Vi phạm quy chế sử dụng", "Tài khoản đã nghỉ việc hoặc chuyển công tác", "Nghi ngờ bị chiếm quyền truy cập", "Theo yêu cầu của đơn vị quản lý"];

const initials = (name: string) => name.trim().split(/\s+/).slice(-2).map(part => part[0]).join("").toLocaleUpperCase("vi-VN");
const formatDate = (iso: string) => new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", dateStyle: "short" }).format(new Date(iso));

export function AdminUsers() {
  return <Suspense fallback={null}><AdminUsersContent /></Suspense>;
}

function AdminUsersContent() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const me = useAuthStore(state => state.user);
  const rawRole = params.get("role");
  const role = ROLES.includes(rawRole as Role) ? rawRole as Role : undefined;
  const rawStatus = params.get("status");
  const status = rawStatus === "ACTIVE" || rawStatus === "LOCKED" ? rawStatus : undefined;
  const q = params.get("q") ?? "";
  const rawPage = Number(params.get("page") ?? "1");
  const page = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1;
  const [search, setSearch] = useState(q);
  const [openId, setOpenId] = useState<string | null>(null);

  function navigate(next: { role?: Role | null; status?: string | null; q?: string; page?: number }) {
    const query = new URLSearchParams();
    const r = next.role === undefined ? role : next.role;
    const s = next.status === undefined ? status : next.status;
    const keyword = next.q ?? q;
    if (r) query.set("role", r);
    if (s) query.set("status", s);
    if (keyword) query.set("q", keyword);
    if ((next.page ?? 1) > 1) query.set("page", String(next.page));
    setOpenId(null);
    router.replace(`${pathname}${query.size ? `?${query}` : ""}`, { scroll: false });
  }

  useEffect(() => {
    const trimmed = search.trim();
    if (trimmed === q) return;
    const timer = setTimeout(() => navigate({ q: trimmed, page: 1 }), 300);
    return () => clearTimeout(timer);
  });

  const summary = useAdminUserSummary();
  const list = useAdminUsers({ q, role, status, page, size: PAGE_SIZE });
  const lastPage = list.data ? Math.max(1, Math.ceil(list.data.total / list.data.size)) : 1;

  return <div className="adm">
    <TaskBanner eyebrow="Quản trị hệ thống" title="Tài khoản" illustration="applications" compact
      description={summary.data ? `${summary.data.total} tài khoản trên hệ thống, ${summary.data.locked} tài khoản đang bị khoá.` : "Xem, tìm kiếm và khoá hoặc mở khoá tài khoản của mọi vai trò."} />

    <div className="adm-tabs" role="tablist" aria-label="Lọc theo vai trò">
      <button type="button" role="tab" className="adm-tab" aria-selected={!role} onClick={() => navigate({ role: null, page: 1 })}>Tất cả<span>{summary.data?.total ?? "–"}</span></button>
      {ROLES.map(r => <button key={r} type="button" role="tab" className="adm-tab" aria-selected={role === r} onClick={() => navigate({ role: r, page: 1 })}>
        {ROLE_LABEL[r]}<span>{summary.data?.byRole[r] ?? "–"}</span></button>)}
    </div>

    <div className="adm-toolbar">
      <label className="adm-search"><Search aria-hidden="true" /><span className="sr-only">Tìm tài khoản</span>
        <input type="search" value={search} onChange={event => setSearch(event.target.value)} placeholder="Tìm theo họ tên, email hoặc mã LMS" /></label>
      <SelectMenu aria-label="Lọc theo trạng thái" value={status ?? ""} className="ui-select-trigger w-auto min-w-44"
        onValueChange={value => navigate({ status: value || null, page: 1 })}
        options={[{ value: "", label: "Mọi trạng thái" }, { value: "ACTIVE", label: "Đang hoạt động" }, { value: "LOCKED", label: "Đang khoá" }]} />
      {list.data && <span className="adm-muted">{list.data.total} tài khoản</span>}
    </div>

    {list.error ? <div role="alert" className="adm-panel adm-panel-body"><p>{getErrorMessage(list.error)}</p><Button variant="outline" className="mt-3" onClick={() => list.refetch()}>Thử lại</Button></div>
      : list.isPending ? <div className="space-y-3" role="status" aria-label="Đang tải">{[0, 1, 2, 3].map(i => <div key={i} className="h-14 rounded-xl bg-muted motion-safe:animate-pulse" />)}</div>
      : list.data.items.length === 0 ? <p className="adm-panel adm-empty justify-center py-10"><Users aria-hidden="true" />Không có tài khoản khớp bộ lọc.</p>
      : <>
        <div className="adm-table-wrap" data-cards aria-busy={list.isPlaceholderData}>
          <table className="adm-table">
            <thead><tr><th>Người dùng</th><th>Vai trò</th><th>Đăng nhập qua</th><th>Ngày tạo</th><th>Trạng thái</th><th><span className="sr-only">Thao tác</span></th></tr></thead>
            <tbody>{list.data.items.map(user => <Fragment key={user.id}>
              <tr data-open={openId === user.id || undefined}>
                <td><Person user={user} isMe={user.id === me?.id} /></td>
                <td><RoleBadge role={user.role} /></td>
                <td>{user.lmsUserId ? <span className="adm-code">{user.lmsUserId}</span> : <span className="adm-muted">Email &amp; mật khẩu</span>}</td>
                <td className="whitespace-nowrap">{formatDate(user.createdAt)}</td>
                <td><StatusBadge user={user} /></td>
                <td><RowActions user={user} isMe={user.id === me?.id} open={openId === user.id} onToggle={() => setOpenId(id => (id === user.id ? null : user.id))} /></td>
              </tr>
              {openId === user.id && <tr data-open><td colSpan={6}><LockPanel user={user} onDone={() => setOpenId(null)} /></td></tr>}
            </Fragment>)}</tbody>
          </table>
        </div>
        <div className="adm-cards">{list.data.items.map(user => <div key={user.id} className="adm-card">
          <div className="flex items-start justify-between gap-2"><Person user={user} isMe={user.id === me?.id} /><StatusBadge user={user} /></div>
          <div className="flex flex-wrap items-center gap-2"><RoleBadge role={user.role} />{user.lmsUserId && <span className="adm-code">{user.lmsUserId}</span>}</div>
          <RowActions user={user} isMe={user.id === me?.id} open={openId === user.id} onToggle={() => setOpenId(id => (id === user.id ? null : user.id))} />
          {openId === user.id && <LockPanel user={user} onDone={() => setOpenId(null)} />}
        </div>)}</div>
        {list.data.total > list.data.size && <nav className="adm-pager" aria-label="Phân trang">
          <span>Trang {page} / {lastPage}</span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => navigate({ page: page - 1 })}>Trước</Button>
            <Button variant="outline" size="sm" disabled={page >= lastPage} onClick={() => navigate({ page: page + 1 })}>Sau</Button>
          </div>
        </nav>}
      </>}
  </div>;
}

function Person({ user, isMe }: { user: AdminUser; isMe: boolean }) {
  const tone = ROLE_TONE[user.role];
  return <div className="adm-person">
    <span className="adm-avatar" style={{ background: tone.bg, color: tone.fg }} aria-hidden="true">{initials(user.fullName)}</span>
    <div className="min-w-0"><strong>{user.fullName}{isMe && <span className="adm-you">Bạn</span>}</strong><small>{user.email}</small></div>
  </div>;
}

export function RoleBadge({ role }: { role: Role }) {
  const tone = ROLE_TONE[role];
  return <span className="adm-role-badge" style={{ background: tone.bg, color: tone.fg }}>{ROLE_LABEL[role]}</span>;
}

function StatusBadge({ user }: { user: AdminUser }) {
  return user.status === "LOCKED"
    ? <span className="adm-badge" data-tone="locked" title={user.lockedReason ?? undefined}><Lock aria-hidden="true" />Đang khoá</span>
    : <span className="adm-badge" data-tone="active"><CheckCircle2 aria-hidden="true" />Hoạt động</span>;
}

function RowActions({ user, isMe, open, onToggle }: { user: AdminUser; isMe: boolean; open: boolean; onToggle(): void }) {
  // Backend chặn tự khoá mình và khoá quản trị khác; giao diện tắt nút sẵn và nói lý do.
  const blocked = isMe ? "Không thể tự khoá tài khoản của mình" : user.role === "ADMIN" ? "Không khoá tài khoản quản trị" : null;
  if (user.status === "LOCKED") return <div className="adm-actions"><button type="button" className="adm-btn" data-tone="success" aria-expanded={open} onClick={onToggle}><LockOpen aria-hidden="true" />Mở khoá</button></div>;
  return <div className="adm-actions"><button type="button" className="adm-btn" data-tone="danger" disabled={Boolean(blocked)} title={blocked ?? undefined} aria-expanded={open} onClick={onToggle}>
    <Lock aria-hidden="true" />Khoá</button></div>;
}

function LockPanel({ user, onDone }: { user: AdminUser; onDone(): void }) {
  const toggle = useToggleUserLock();
  const [reason, setReason] = useState("");
  const locking = user.status !== "LOCKED";
  return <div className="adm-inline">
    {locking ? <>
      <p className="text-sm"><strong>Khoá tài khoản {user.fullName}?</strong> Người dùng bị đăng xuất ở lần mở trang tiếp theo và không đăng nhập lại được (cả email lẫn SSO qua LMS) cho tới khi được mở khoá.</p>
      <div className="adm-quick">{QUICK_REASONS.map(text => <button key={text} type="button" onClick={() => setReason(text)}>{text}</button>)}</div>
      <textarea className="adm-input" aria-label="Lý do khoá" maxLength={500} value={reason} onChange={event => setReason(event.target.value)} placeholder="Lý do khoá (bắt buộc, lưu lại để tra cứu)…" />
    </> : <p className="text-sm"><strong>Mở khoá tài khoản {user.fullName}?</strong>{user.lockedReason ? ` Lý do khoá trước đó: ${user.lockedReason}` : ""}</p>}
    {toggle.isError && <p role="alert" className="adm-error flex items-center gap-1.5"><AlertCircle className="size-4" aria-hidden="true" />{getErrorMessage(toggle.error)}</p>}
    <div className="flex flex-wrap gap-2">
      <button type="button" className="adm-btn" onClick={onDone} disabled={toggle.isPending}>Huỷ</button>
      <button type="button" className="adm-btn" data-tone={locking ? "primary" : "success"} disabled={toggle.isPending || (locking && !reason.trim())}
        onClick={() => toggle.mutate(locking ? { id: user.id, lock: true, reason: reason.trim() } : { id: user.id, lock: false }, { onSuccess: onDone })}>
        {locking ? <Lock aria-hidden="true" /> : <LockOpen aria-hidden="true" />}{toggle.isPending ? "Đang xử lý…" : locking ? "Xác nhận khoá" : "Xác nhận mở khoá"}</button>
    </div>
  </div>;
}
