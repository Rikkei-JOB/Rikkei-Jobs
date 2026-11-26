"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, FileCheck2, GraduationCap, Inbox, Lock, ShieldCheck, Sparkles, UserCog, UserSearch, Users } from "lucide-react";
import { TaskBanner } from "@/app/components/ui/task-banner";
import { ROLE_LABEL } from "@/config/roles";
import { useAdminUserSummary, useAdminUsers, useSkillCatalogAdmin } from "@/hooks/use-admin";
import { useAuthStore } from "@/hooks/use-auth-store";
import { useStaffJobSummary } from "@/hooks/use-staff-jobs";
import type { Role } from "@/interface/auth";
import { hrApplicationService } from "@/services/hr-application.service";
import { ROLE_TONE, RoleBadge } from "./admin-users";
import "./admin.css";

const ROLE_ICON: Record<Role, typeof Users> = { CANDIDATE: GraduationCap, HR: UserSearch, MENTOR: ShieldCheck, STAFF: UserCog, ADMIN: Lock };
const ROLES: Role[] = ["CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN"];

/** Tổng quan quản trị: số liệu toàn hệ thống từ API quản trị và các API cán bộ mà ADMIN được dùng. */
export function AdminOverview() {
  const user = useAuthStore(state => state.user);
  const users = useAdminUserSummary();
  const locked = useAdminUsers({ status: "LOCKED", page: 1, size: 5 });
  const jobs = useStaffJobSummary();
  const skills = useSkillCatalogAdmin();
  const applications = useQuery({ queryKey: ["admin", user?.id ?? null, "applications-total"], enabled: Boolean(user),
    queryFn: async () => (await hrApplicationService.listStaff({ page: 1, size: 1 })).total });

  const firstName = user?.fullName?.trim().split(/\s+/).at(-1) ?? "quản trị viên";
  const groups = new Set((skills.data ?? []).map(skill => skill.group)).size;
  const stats = [
    { label: "Tài khoản", value: users.data?.total, sub: users.data ? `${users.data.locked} tài khoản đang khoá` : "", icon: Users, href: "/admin/users", tone: "blue" },
    { label: "JD chờ duyệt", value: jobs.data?.byStatus.PENDING, sub: jobs.data ? `${jobs.data.byStatus.APPROVED} JD đang tuyển` : "", icon: FileCheck2, href: "/admin/jobs", tone: "amber" },
    { label: "Đơn ứng tuyển", value: applications.data, sub: "Trên toàn nền tảng", icon: UserSearch, href: "/admin/applications", tone: "green" },
    { label: "Kỹ năng", value: skills.data?.length, sub: skills.data ? `${groups} nhóm kỹ năng` : "", icon: Sparkles, href: "/admin/skills", tone: "violet" },
  ];

  return <div className="adm">
    <TaskBanner eyebrow="Bảng điều khiển quản trị" title={`Xin chào, ${firstName}`} illustration="applications" compact
      description="Theo dõi tài khoản, danh mục kỹ năng và hoạt động tuyển dụng trên toàn hệ thống.">
      <Link href="/admin/users"><Users className="size-4" aria-hidden="true" />Quản lý tài khoản</Link>
      <Link href="/admin/skills"><Sparkles className="size-4" aria-hidden="true" />Danh mục kỹ năng</Link>
    </TaskBanner>

    <div className="adm-stats">
      {stats.map(stat => <Link key={stat.label} href={stat.href} className="adm-stat" data-tone={stat.tone}>
        <span className="adm-icon"><stat.icon aria-hidden="true" /></span>
        <strong>{stat.value ?? "–"}</strong><span>{stat.label}</span>{stat.sub && <small>{stat.sub}</small>}
      </Link>)}
    </div>

    <div className="adm-columns">
      <section className="adm-panel" aria-label="Tài khoản theo vai trò">
        <div className="adm-panel-head"><h2><Users aria-hidden="true" />Tài khoản theo vai trò</h2><Link href="/admin/users" className="adm-link">Xem tất cả<ArrowRight aria-hidden="true" /></Link></div>
        <div className="adm-panel-body adm-roles">
          {ROLES.map(role => {
            const Icon = ROLE_ICON[role];
            return <Link key={role} href={`/admin/users?role=${role}`} className="adm-role">
              <span className="adm-icon" style={{ background: ROLE_TONE[role].bg, color: ROLE_TONE[role].fg }}><Icon aria-hidden="true" /></span>
              <span><strong>{users.data?.byRole[role] ?? "–"}</strong><span>{ROLE_LABEL[role]}</span></span>
            </Link>;
          })}
        </div>
      </section>

      <section className="adm-panel" aria-label="Tài khoản đang khoá">
        <div className="adm-panel-head"><h2><Lock aria-hidden="true" />Tài khoản đang khoá</h2><Link href="/admin/users?status=LOCKED" className="adm-link">Tất cả<ArrowRight aria-hidden="true" /></Link></div>
        <div className="adm-panel-body">
          {locked.isPending ? <div className="h-28 rounded-xl bg-muted motion-safe:animate-pulse" />
            : !locked.data?.items.length ? <p className="adm-empty"><Inbox aria-hidden="true" />Không có tài khoản nào đang bị khoá.</p>
            : <ul className="adm-list">{locked.data.items.map(item => <li key={item.id} className="adm-list-row">
                <div className="min-w-0 flex-1"><strong className="block text-sm text-[var(--heading)]">{item.fullName}</strong>
                  <span className="block truncate text-xs text-[var(--text-muted)]">{item.lockedReason ?? item.email}</span></div>
                <RoleBadge role={item.role} />
              </li>)}</ul>}
        </div>
      </section>
    </div>
  </div>;
}
