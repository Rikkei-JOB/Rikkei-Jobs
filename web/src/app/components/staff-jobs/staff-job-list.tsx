"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { AlertCircle, ArrowRight, Building2, CalendarClock, ClipboardCheck, Lock, Search } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { LEVEL_LABEL } from "@/app/components/job/job-labels";
import { useNow } from "@/hooks/use-now";
import { useStaffJobSummary, useStaffJobs } from "@/hooks/use-staff-jobs";
import type { StaffJobItem, StaffJobStatus } from "@/interface/staff-job";
import { getErrorMessage } from "@/services/api";
import { JobsBase, STATUS_INFO, deadlineText, timeAgo, useJobsBase } from "./staff-job-utils";
import "./staff-jobs.css";
import { TaskBanner } from "@/app/components/ui/task-banner";

const PAGE_SIZE = 20;
const TABS: { key: StaffJobStatus | "ALL"; label: string; hint: string }[] = [
  { key: "PENDING", label: "Chờ duyệt", hint: "Cần xử lý" },
  { key: "APPROVED", label: "Đang tuyển", hint: "Đã duyệt" },
  { key: "REJECTED", label: "Bị trả lại", hint: "Chờ HR sửa" },
  { key: "CLOSED", label: "Đã đóng", hint: "Ngừng tuyển" },
  { key: "ALL", label: "Tất cả", hint: "Trừ bản nháp" },
];

export function StaffJobList({ base = "/staff/jobs" }: { base?: string }) {
  return <JobsBase.Provider value={base}><Suspense fallback={null}><StaffJobListContent /></Suspense></JobsBase.Provider>;
}

function StaffJobListContent() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const now = useNow();
  const rawStatus = params.get("status");
  const tab = TABS.some(t => t.key === rawStatus) ? rawStatus as StaffJobStatus | "ALL" : "PENDING";
  const q = params.get("q") ?? "";
  const rawPage = Number(params.get("page") ?? "1");
  const page = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1;
  const [search, setSearch] = useState(q);

  function navigate(next: { status?: string; q?: string; page?: number }) {
    const query = new URLSearchParams();
    const status = next.status ?? tab;
    const keyword = next.q ?? q;
    if (status !== "PENDING") query.set("status", status);
    if (keyword) query.set("q", keyword);
    if ((next.page ?? 1) > 1) query.set("page", String(next.page));
    router.replace(`${pathname}${query.size ? `?${query}` : ""}`, { scroll: false });
  }

  // Ô tìm đẩy lên URL sau 300 ms ngừng gõ (giống trang Doanh nghiệp).
  useEffect(() => {
    const trimmed = search.trim();
    if (trimmed === q) return;
    const timer = setTimeout(() => navigate({ q: trimmed, page: 1 }), 300);
    return () => clearTimeout(timer);
  });

  const summary = useStaffJobSummary();
  const list = useStaffJobs({ status: tab === "ALL" ? undefined : tab, q, page, size: PAGE_SIZE });
  const lastPage = list.data ? Math.max(1, Math.ceil(list.data.total / list.data.size)) : 1;
  const count = (key: StaffJobStatus | "ALL") => summary.data ? (key === "ALL" ? summary.data.total : summary.data.byStatus[key]) : undefined;

  return <div className="sjr space-y-5">
    <TaskBanner eyebrow="Kiểm duyệt nội dung" title="Duyệt JD" illustration="applications" compact
      description="Kiểm tra tin tuyển dụng doanh nghiệp gửi lên trước khi hiển thị cho ứng viên trên trang việc làm." />

    <div className="sjr-tabs" role="tablist" aria-label="Lọc theo trạng thái">
      {TABS.map(t => <button key={t.key} type="button" role="tab" aria-selected={tab === t.key} className="sjr-tab" data-tone={t.key === "PENDING" ? "pending" : undefined}
        onClick={() => navigate({ status: t.key, page: 1 })}>
        <strong>{count(t.key) ?? "–"}</strong><span>{t.label}</span><small>{t.hint}</small>
      </button>)}
    </div>

    <div className="sjr-toolbar">
      <label className="sjr-search"><Search aria-hidden="true" /><span className="sr-only">Tìm JD</span>
        <input type="search" value={search} onChange={event => setSearch(event.target.value)} placeholder="Tìm theo tiêu đề, vị trí hoặc tên công ty" /></label>
      {tab === "PENDING" && <p className="sjr-note">Xếp theo thời gian gửi, JD gửi sớm nhất ở trên cùng.</p>}
    </div>

    {list.error ? <div role="alert" className="sjr-empty"><AlertCircle aria-hidden="true" /><p>{getErrorMessage(list.error)}</p>
        <Button variant="outline" onClick={() => list.refetch()}>Thử lại</Button></div>
      : list.isPending ? <div className="space-y-3" role="status" aria-label="Đang tải">{[0, 1, 2].map(i => <div key={i} className="h-20 rounded-xl bg-muted motion-safe:animate-pulse" />)}</div>
      : list.data.items.length === 0 ? <div className="sjr-empty"><ClipboardCheck aria-hidden="true" />
          <h2>{q ? "Không có JD khớp từ khoá" : tab === "PENDING" ? "Không còn JD chờ duyệt" : "Chưa có JD ở trạng thái này"}</h2>
          <p>{tab === "PENDING" && !q ? "Khi doanh nghiệp gửi JD mới, JD sẽ xuất hiện tại đây." : "Thử đổi bộ lọc hoặc từ khoá."}</p></div>
      : <>
        <div className="sjr-table-wrap" aria-busy={list.isPlaceholderData}>
          <table className="sjr-table">
            <thead><tr><th>JD</th><th>Cấp bậc</th><th>Hạn nộp</th><th>{tab === "PENDING" ? "Gửi duyệt" : "Cập nhật"}</th><th>Trạng thái</th><th><span className="sr-only">Thao tác</span></th></tr></thead>
            <tbody>{list.data.items.map(job => <JobRow key={job.id} job={job} now={now} />)}</tbody>
          </table>
        </div>
        <div className="sjr-cards">{list.data.items.map(job => <JobCard key={job.id} job={job} now={now} />)}</div>
        {list.data.total > list.data.size && <nav className="sjr-pager" aria-label="Phân trang">
          <span>Trang {page} / {lastPage} · {list.data.total} JD</span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => navigate({ page: page - 1 })}>Trước</Button>
            <Button variant="outline" size="sm" disabled={page >= lastPage} onClick={() => navigate({ page: page + 1 })}>Sau</Button>
          </div>
        </nav>}
      </>}
  </div>;
}

function StatusBadge({ status }: { status: StaffJobStatus }) {
  return <span className="sjr-badge" data-tone={STATUS_INFO[status].tone}>{STATUS_INFO[status].label}</span>;
}

function CompanyLine({ job }: { job: StaffJobItem }) {
  return <span className="sjr-company"><Building2 aria-hidden="true" />{job.companyName ?? "—"}
    {job.companyStatus === "LOCKED" && <span className="sjr-locked"><Lock aria-hidden="true" />Đang khoá</span>}</span>;
}

function JobRow({ job, now }: { job: StaffJobItem; now: number }) {
  const deadline = deadlineText(job.deadline, now);
  const href = `${useJobsBase()}/${job.id}`;
  return <tr>
    <td><Link href={href} className="sjr-title">{job.title}</Link><CompanyLine job={job} />
      <span className="sjr-sub">{job.rejectReason ? `Lý do trả lại: ${job.rejectReason}` : `${job.requiredSkillCount} kỹ năng bắt buộc`}</span></td>
    <td>{job.level ? LEVEL_LABEL[job.level] : "—"}</td>
    <td><span className="sjr-deadline" data-tone={deadline.tone}><CalendarClock aria-hidden="true" />{deadline.text}</span></td>
    <td className="whitespace-nowrap">{timeAgo(job.submittedAt, now)}</td>
    <td><StatusBadge status={job.status} /></td>
    <td><Link href={href} className="sjr-open">{job.status === "PENDING" ? "Xem & duyệt" : "Xem"}<ArrowRight aria-hidden="true" /></Link></td>
  </tr>;
}

function JobCard({ job, now }: { job: StaffJobItem; now: number }) {
  const base = useJobsBase();
  const deadline = deadlineText(job.deadline, now);
  return <Link href={`${base}/${job.id}`} className="sjr-card">
    <div className="flex items-start justify-between gap-3"><strong className="sjr-title">{job.title}</strong><StatusBadge status={job.status} /></div>
    <CompanyLine job={job} />
    <div className="sjr-card-meta">
      <span>{job.level ? LEVEL_LABEL[job.level] : "—"}</span>
      <span className="sjr-deadline" data-tone={deadline.tone}><CalendarClock aria-hidden="true" />{deadline.text}</span>
      <span>{timeAgo(job.submittedAt, now)}</span>
    </div>
  </Link>;
}
