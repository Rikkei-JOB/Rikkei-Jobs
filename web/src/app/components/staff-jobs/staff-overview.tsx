"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, Building2, CheckCircle2, ClipboardList, FileCheck2, Inbox, RefreshCw, UserSearch, Users, XCircle } from "lucide-react";
import { useAuthStore } from "@/hooks/use-auth-store";
import { useNow } from "@/hooks/use-now";
import { useStaffJobSummary, useStaffJobs } from "@/hooks/use-staff-jobs";
import { hrApplicationService } from "@/services/hr-application.service";
import { profileReviewService } from "@/services/profile-review.service";
import { staffCompanyService } from "@/services/staff-company.service";
import { syncService } from "@/services/sync.service";
import { deadlineText, formatDateTime, timeAgo } from "./staff-job-utils";
import "./staff-jobs.css";

/**
 * Tổng quan cán bộ: gom số liệu từ API sẵn có (DN-07 JD, hàng chờ duyệt hồ sơ, đơn ứng tuyển, doanh nghiệp, đồng bộ).
 * Mỗi khối tải riêng, khối lỗi chỉ hiện "–" chứ không làm hỏng cả trang.
 */
export function StaffOverview() {
  const user = useAuthStore(state => state.user);
  const userId = user?.id ?? null;
  const now = useNow();
  const jobSummary = useStaffJobSummary();
  const pendingJobs = useStaffJobs({ status: "PENDING", page: 1, size: 5 });
  const reviews = useQuery({ queryKey: ["staff-overview", userId, "profile-reviews"], enabled: Boolean(userId),
    queryFn: () => profileReviewService.queue({ q: "", classCode: "", courseName: "", page: 1 }) });
  const applications = useQuery({ queryKey: ["staff-overview", userId, "applications"], enabled: Boolean(userId),
    queryFn: async () => {
      const [all, interview] = await Promise.all([hrApplicationService.listStaff({ page: 1, size: 1 }), hrApplicationService.listStaff({ status: "INTERVIEW", page: 1, size: 1 })]);
      return { total: all.total, interview: interview.total };
    } });
  const companies = useQuery({ queryKey: ["staff-overview", userId, "companies"], enabled: Boolean(userId),
    queryFn: async () => {
      const [all, locked] = await Promise.all([staffCompanyService.list({ page: 1, size: 1 }), staffCompanyService.list({ status: "LOCKED", page: 1, size: 1 })]);
      return { total: all.total, locked: locked.total };
    } });
  const lastSync = useQuery({ queryKey: ["staff-overview", userId, "sync"], enabled: Boolean(userId),
    queryFn: async () => (await syncService.listRuns({ page: 1, size: 1 })).items[0] ?? null });

  const firstName = user?.fullName?.trim().split(/\s+/).at(-1) ?? "cán bộ";
  const pendingJd = jobSummary.data?.byStatus.PENDING;
  const pendingProfiles = reviews.data?.total;
  const todo = (pendingJd ?? 0) + (pendingProfiles ?? 0);

  const stats = [
    { label: "JD chờ duyệt", value: pendingJd, sub: jobSummary.data ? `${jobSummary.data.byStatus.APPROVED} JD đang tuyển` : "", icon: FileCheck2, href: "/staff/jobs", tone: "pending" },
    { label: "Hồ sơ chờ duyệt", value: pendingProfiles, sub: "Ứng viên xin công khai hồ sơ", icon: Users, href: "/staff/profile-reviews", tone: "brand" },
    { label: "Đơn ứng tuyển", value: applications.data?.total, sub: applications.data ? `${applications.data.interview} đơn đang phỏng vấn` : "", icon: UserSearch, href: "/staff/applications", tone: "blue" },
    { label: "Doanh nghiệp", value: companies.data?.total, sub: companies.data ? `${companies.data.locked} công ty đang khoá` : "", icon: Building2, href: "/staff/companies", tone: "green" },
  ];

  return <div className="sjr space-y-6">
    <header className="sov-hero">
      <div>
        <p className="sjr-eyebrow">Không gian cán bộ tuyển dụng</p>
        <h1>Xin chào, {firstName}</h1>
        <p>{jobSummary.isPending || reviews.isPending ? "Đang tổng hợp công việc của bạn…"
          : todo ? `Có ${todo} mục đang chờ bạn xử lý: ${pendingJd ?? 0} JD và ${pendingProfiles ?? 0} hồ sơ ứng viên.` : "Không còn mục nào chờ duyệt. Tuyệt vời!"}</p>
      </div>
      <div className="sov-hero-actions">
        <Link href="/staff/jobs"><FileCheck2 aria-hidden="true" />Duyệt JD</Link>
        <Link href="/staff/profile-reviews"><ClipboardList aria-hidden="true" />Duyệt hồ sơ</Link>
      </div>
    </header>

    <div className="sov-stats">
      {stats.map(stat => <Link key={stat.label} href={stat.href} className="sov-stat" data-tone={stat.tone}>
        <span className="sov-stat-icon"><stat.icon aria-hidden="true" /></span>
        <strong>{stat.value ?? "–"}</strong><span>{stat.label}</span>{stat.sub && <small>{stat.sub}</small>}
      </Link>)}
    </div>

    <div className="sov-columns">
      <section className="sjr-panel" aria-label="JD chờ duyệt">
        <div className="sov-panel-head"><h2>JD chờ duyệt</h2><Link href="/staff/jobs" className="sjr-open">Tất cả<ArrowRight aria-hidden="true" /></Link></div>
        {pendingJobs.isPending ? <div className="h-40 rounded-xl bg-muted motion-safe:animate-pulse" />
          : !pendingJobs.data?.items.length ? <p className="sov-empty"><Inbox aria-hidden="true" />Không có JD nào đang chờ duyệt.</p>
          : <ul className="sov-list">{pendingJobs.data.items.map(job => {
              const deadline = deadlineText(job.deadline, now);
              return <li key={job.id}><Link href={`/staff/jobs/${job.id}`}>
                <span className="min-w-0"><strong>{job.title}</strong><small>{job.companyName} · gửi {timeAgo(job.submittedAt, now)}</small></span>
                <span className="sjr-deadline" data-tone={deadline.tone}>{deadline.text}</span>
              </Link></li>;
            })}</ul>}
      </section>

      <section className="sjr-panel" aria-label="Hồ sơ chờ duyệt">
        <div className="sov-panel-head"><h2>Hồ sơ chờ duyệt</h2><Link href="/staff/profile-reviews" className="sjr-open">Tất cả<ArrowRight aria-hidden="true" /></Link></div>
        {reviews.isPending ? <div className="h-40 rounded-xl bg-muted motion-safe:animate-pulse" />
          : reviews.error ? <p className="sov-empty">Chưa tải được hàng chờ hồ sơ.</p>
          : !reviews.data?.items.length ? <p className="sov-empty"><Inbox aria-hidden="true" />Không có hồ sơ nào đang chờ duyệt.</p>
          : <ul className="sov-list">{reviews.data.items.slice(0, 5).map(row => <li key={row.userId}><Link href={`/staff/profile-reviews/${row.userId}`}>
              <span className="min-w-0"><strong>{row.fullName}</strong><small>{[row.classCode, row.courseName].filter(Boolean).join(" · ")}</small></span>
              {row.consentAt && <span className="text-xs text-[var(--sjr-muted)] whitespace-nowrap">{timeAgo(row.consentAt, now)}</span>}
            </Link></li>)}</ul>}
      </section>
    </div>

    <section className="sjr-panel sov-sync" aria-label="Đồng bộ dữ liệu">
      <span className="sov-stat-icon" data-tone="blue"><RefreshCw aria-hidden="true" /></span>
      <div className="min-w-0 flex-1">
        <h2>Đồng bộ dữ liệu LMS, Khảo thí, Thực tập</h2>
        <p>{lastSync.isPending ? "Đang tải…" : lastSync.data
          ? <>Lần gần nhất {timeAgo(lastSync.data.finishedAt, now)} ({formatDateTime(lastSync.data.finishedAt)}){lastSync.data.candidateName ? ` · ${lastSync.data.candidateName}` : " · toàn bộ ứng viên"}</>
          : "Chưa có lần đồng bộ nào."}</p>
      </div>
      {lastSync.data && <span className="sjr-badge" data-tone={lastSync.data.status === "SUCCESS" ? "approved" : "rejected"}>
        {lastSync.data.status === "SUCCESS" ? <><CheckCircle2 className="mr-1 size-3.5" aria-hidden="true" />Thành công</> : <><XCircle className="mr-1 size-3.5" aria-hidden="true" />Thất bại</>}</span>}
      <Link href="/staff/sync" className="sjr-open">Mở trang đồng bộ<ArrowRight aria-hidden="true" /></Link>
    </section>
  </div>;
}
