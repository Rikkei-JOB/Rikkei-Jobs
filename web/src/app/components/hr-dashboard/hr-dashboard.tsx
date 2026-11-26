"use client";

import { useMemo } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, ArrowRight, Building2, CalendarClock, CalendarDays, ChevronRight, Clock, FilePlus2, FileText, Inbox, Undo2, UserSearch, Video } from "lucide-react";
import { TaskBanner } from "@/app/components/ui/task-banner";
import { CompanyMark } from "@/app/components/company/company-mark";
import { APPLICATION_STATUS } from "@/config/application-status";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { useAuthStore } from "@/hooks/use-auth-store";
import { useMyCompany } from "@/hooks/use-my-company";
import { useMyJobSummary, useMyJobs, useOpenJobs } from "@/hooks/use-my-jobs";
import { useNow } from "@/hooks/use-now";
import type { ApplicationStatus } from "@/interface/application";
import { hrApplicationService } from "@/services/hr-application.service";
import { interviewService } from "@/services/interview.service";
import "./hr-dashboard.css";

const ZONE = "Asia/Ho_Chi_Minh";
const DAY = 86_400_000;
/** Các bước chính của quy trình tuyển dụng (UT-02), theo thứ tự. */
const PIPELINE: ApplicationStatus[] = ["SUBMITTED", "SCREENING", "INTERVIEW", "OFFER", "HIRED"];

const fmt = (iso: string, options: Intl.DateTimeFormatOptions) => new Intl.DateTimeFormat("vi-VN", { timeZone: ZONE, ...options }).format(new Date(iso));
const todayKey = (now: number) => new Intl.DateTimeFormat("en-CA", { timeZone: ZONE }).format(new Date(now));
function daysUntil(date: string, now: number) {
  return Math.round((Date.parse(`${date.slice(0, 10)}T00:00:00Z`) - Date.parse(`${todayKey(now)}T00:00:00Z`)) / DAY);
}

/**
 * Bảng điều khiển của nhà tuyển dụng: ghép số liệu thật từ API sẵn có của công ty
 * (companies/me, JD theo trạng thái, đơn ứng tuyển theo bước, lịch phỏng vấn 14 ngày tới).
 */
export function HrDashboard() {
  const allowed = useAuthGuard({ roles: ["HR"] });
  const user = useAuthStore(state => state.user);
  const now = useNow();
  const company = useMyCompany(allowed);
  const summary = useMyJobSummary(allowed);
  const openJobs = useOpenJobs(allowed);
  const rejected = useMyJobs("REJECTED", 1, allowed);
  const pipeline = useQuery({
    queryKey: ["hr-dashboard", user?.id ?? null, "pipeline"], enabled: allowed && Boolean(user),
    queryFn: async () => {
      const counts = await Promise.all([...PIPELINE, "REJECTED" as const].map(status => hrApplicationService.listHr({ status, page: 1, size: 1 }).then(page => [status, page.total] as const)));
      return Object.fromEntries(counts) as Record<ApplicationStatus, number>;
    },
  });
  // Mốc tuần tính theo phút để khoá cache ổn định; useNow cập nhật mỗi phút.
  const from = new Date(Math.floor(now / 60_000) * 60_000).toISOString();
  const to = new Date(Math.floor(now / 60_000) * 60_000 + 14 * DAY).toISOString();
  const interviews = useQuery({
    queryKey: ["hr-dashboard", user?.id ?? null, "interviews", from.slice(0, 13)], enabled: allowed && Boolean(user),
    queryFn: ({ signal }) => interviewService.companyAll({ from, to, status: "SCHEDULED" }, signal),
  });

  const upcoming = useMemo(() => (interviews.data ?? []).filter(item => Date.parse(item.schedule.endAt) >= now)
    .sort((a, b) => a.schedule.startAt.localeCompare(b.schedule.startAt)), [interviews.data, now]);
  const expiring = useMemo(() => (openJobs.data ?? []).filter(job => job.deadline && daysUntil(job.deadline, now) <= 7 && daysUntil(job.deadline, now) >= 0)
    .sort((a, b) => (a.deadline ?? "").localeCompare(b.deadline ?? "")), [openJobs.data, now]);
  const attention = [
    ...(rejected.data?.items ?? []).map(job => ({ id: job.id, title: job.title, kind: "rejected" as const, note: job.rejectReason ?? "Cán bộ đã trả lại JD" })),
    ...expiring.map(job => ({ id: job.id, title: job.title, kind: "expiring" as const, note: daysUntil(job.deadline!, now) === 0 ? "Hết hạn nộp hôm nay" : `Còn ${daysUntil(job.deadline!, now)} ngày hết hạn nộp` })),
  ].slice(0, 5);

  if (!allowed) return null;

  const hrName = user?.fullName?.trim() || "nhà tuyển dụng";
  const newApplications = pipeline.data?.SUBMITTED;
  const description = summary.data && pipeline.data
    ? `${company.data?.name ?? "Công ty"} có ${summary.data.byStatus.APPROVED} tin đang tuyển, ${newApplications ?? 0} hồ sơ mới cần sàng lọc và ${upcoming.length} buổi phỏng vấn trong 14 ngày tới.`
    : "Đang tổng hợp hoạt động tuyển dụng của công ty…";

  const stats = [
    { label: "Tin đang tuyển", value: summary.data?.byStatus.APPROVED, sub: summary.data ? `${summary.data.expiringSoon} tin sắp hết hạn` : "", icon: FileText, href: "/hr/jobs?status=APPROVED", tone: "green" },
    { label: "Chờ cán bộ duyệt", value: summary.data?.byStatus.PENDING, sub: summary.data ? `${summary.data.byStatus.REJECTED} tin bị trả lại` : "", icon: Clock, href: "/hr/jobs?status=PENDING", tone: "amber" },
    { label: "Hồ sơ mới", value: newApplications, sub: "Cần sàng lọc", icon: UserSearch, href: "/hr/recruitment?status=SUBMITTED", tone: "brand" },
    { label: "Phỏng vấn sắp tới", value: interviews.data ? upcoming.length : undefined, sub: "Trong 14 ngày tới", icon: CalendarDays, href: "/hr/interviews", tone: "blue" },
  ];
  const pipelineMax = Math.max(1, ...PIPELINE.map(status => pipeline.data?.[status] ?? 0));

  return <div className="hr-page hrd">
    <TaskBanner eyebrow="Không gian nhà tuyển dụng" title={`Xin chào, ${hrName}`} description={description} illustration="dashboard">
      <Link href="/hr/jobs/new" data-primary=""><FilePlus2 className="size-4" aria-hidden="true" />Tạo tin tuyển dụng</Link>
      <Link href="/hr/recruitment"><UserSearch className="size-4" aria-hidden="true" />Xem hồ sơ ứng tuyển</Link>
    </TaskBanner>

    <div className="hrd-stats">
      {stats.map(stat => <Link key={stat.label} href={stat.href} className="hrd-stat" data-tone={stat.tone}>
        <span className="hrd-icon"><stat.icon aria-hidden="true" /></span>
        <strong>{stat.value ?? "–"}</strong><span>{stat.label}</span>{stat.sub && <small>{stat.sub}</small>}
      </Link>)}
    </div>

    <section className="hrd-panel" aria-labelledby="hrd-pipeline">
      <div className="hrd-panel-head"><h2 id="hrd-pipeline"><UserSearch aria-hidden="true" />Quy trình tuyển dụng</h2>
        <Link href="/hr/recruitment" className="hrd-link">Mở danh sách hồ sơ<ArrowRight aria-hidden="true" /></Link></div>
      <ol className="hrd-pipeline">
        {PIPELINE.map((status, index) => {
          const count = pipeline.data?.[status];
          return <li key={status}>
            <Link href={`/hr/recruitment?status=${status}`} className="hrd-step" data-status={status}>
              <span className="hrd-step-index">{index + 1}</span>
              <strong>{count ?? "–"}</strong>
              <span>{APPLICATION_STATUS[status].label}</span>
              <span className="hrd-step-bar" aria-hidden="true"><i style={{ width: `${((count ?? 0) / pipelineMax) * 100}%` }} /></span>
            </Link>
            {index < PIPELINE.length - 1 && <ChevronRight className="hrd-step-arrow" aria-hidden="true" />}
          </li>;
        })}
      </ol>
      {pipeline.data && <p className="hrd-note">{pipeline.data.REJECTED} hồ sơ không đạt · số liệu tính trên toàn bộ tin tuyển dụng của công ty.</p>}
    </section>

    <div className="hrd-columns">
      <section className="hrd-panel" aria-labelledby="hrd-interviews">
        <div className="hrd-panel-head"><h2 id="hrd-interviews"><CalendarClock aria-hidden="true" />Phỏng vấn sắp tới</h2><Link href="/hr/interviews" className="hrd-link">Lịch tuần<ArrowRight aria-hidden="true" /></Link></div>
        <div className="hrd-panel-body">
          {interviews.isPending ? <div className="h-40 rounded-xl bg-muted motion-safe:animate-pulse" />
            : !upcoming.length ? <p className="hrd-empty"><Inbox aria-hidden="true" />Chưa có buổi phỏng vấn nào trong 14 ngày tới.</p>
            : <ul className="hrd-list">{upcoming.slice(0, 5).map(item => <li key={item.schedule.id} className="hrd-interview">
                <span className="hrd-date" data-today={todayKey(now) === new Intl.DateTimeFormat("en-CA", { timeZone: ZONE }).format(new Date(item.schedule.startAt)) || undefined}>
                  <small>{fmt(item.schedule.startAt, { weekday: "short" })}</small><strong>{fmt(item.schedule.startAt, { day: "2-digit" })}</strong><small>Th {Number(fmt(item.schedule.startAt, { month: "numeric" }))}</small></span>
                <div className="min-w-0 flex-1"><strong>{item.candidateName}</strong>
                  <small>{item.jobTitle} · {item.roundName}</small>
                  <small>{fmt(item.schedule.startAt, { hour: "2-digit", minute: "2-digit" })} – {fmt(item.schedule.endAt, { hour: "2-digit", minute: "2-digit" })} · {item.interviewerEmail}</small></div>
                {item.schedule.meetUrl && <a href={item.schedule.meetUrl} target="_blank" rel="noopener noreferrer" className="hrd-meet" aria-label={`Vào phòng họp với ${item.candidateName}`}><Video aria-hidden="true" />Vào họp</a>}
              </li>)}</ul>}
        </div>
      </section>

      <section className="hrd-panel" aria-labelledby="hrd-attention">
        <div className="hrd-panel-head"><h2 id="hrd-attention"><AlertTriangle aria-hidden="true" />Tin cần chú ý</h2><Link href="/hr/jobs" className="hrd-link">Tất cả tin<ArrowRight aria-hidden="true" /></Link></div>
        <div className="hrd-panel-body">
          {rejected.isPending || openJobs.isPending ? <div className="h-40 rounded-xl bg-muted motion-safe:animate-pulse" />
            : !attention.length ? <p className="hrd-empty"><Inbox aria-hidden="true" />Không có tin nào bị trả lại hoặc sắp hết hạn.</p>
            : <ul className="hrd-list">{attention.map(item => <li key={`${item.kind}-${item.id}`}><Link href={`/hr/jobs/${item.id}`} className="hrd-attention" data-kind={item.kind}>
                <span className="hrd-icon">{item.kind === "rejected" ? <Undo2 aria-hidden="true" /> : <Clock aria-hidden="true" />}</span>
                <span className="min-w-0 flex-1"><strong>{item.title}</strong><small>{item.note}</small></span>
                <ChevronRight className="size-4 shrink-0 text-[var(--text-muted)]" aria-hidden="true" />
              </Link></li>)}</ul>}
        </div>
      </section>
    </div>

    {company.data && <section className="hrd-panel hrd-company" aria-label="Hồ sơ công ty">
      <CompanyMark name={company.data.name} logoUrl={company.data.logoUrl} size={56} brand />
      <div className="min-w-0 flex-1"><h2>{company.data.name}</h2>
        <p>{[company.data.industry, company.data.size ? `${company.data.size} nhân sự` : null, company.data.city].filter(Boolean).join(" · ") || "Chưa cập nhật thông tin công ty"}</p></div>
      <Link href="/hr/company" className="hrd-link"><Building2 aria-hidden="true" />Hồ sơ công ty</Link>
      <Link href="/hr/company/preview" className="hrd-link">Xem như ứng viên<ArrowRight aria-hidden="true" /></Link>
    </section>}
  </div>;
}
