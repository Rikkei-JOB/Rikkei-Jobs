"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { BadgeCheck, CalendarCheck2, ChevronRight, ClipboardCheck, GraduationCap, Inbox, UserSearch } from "lucide-react";
import { TaskBanner } from "@/app/components/ui/task-banner";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { useAuthStore } from "@/hooks/use-auth-store";
import { hrApplicationService } from "@/services/hr-application.service";
import { getErrorMessage } from "@/services/api";
import "./hr-dashboard.css";

const dateFmt = new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeZone: "Asia/Ho_Chi_Minh" });
const initials = (name: string) => name.trim().split(/\s+/).slice(-2).map(part => part[0]).join("").toUpperCase();

/** Lộ trình sau khi nhận việc; phần theo dõi/nghiệm thu chờ phân hệ thực tập (backend chưa có). */
const STEPS = [
  { icon: BadgeCheck, title: "Nhận việc", text: "Ứng viên chấp nhận offer, hồ sơ chuyển sang trạng thái Đã nhận việc.", ready: true },
  { icon: CalendarCheck2, title: "Theo dõi kỳ thực tập", text: "Cán bộ IOC điều phối đợt thực tập và ghép ứng viên vào doanh nghiệp.", ready: false },
  { icon: ClipboardCheck, title: "Nghiệm thu", text: "Doanh nghiệp đánh giá kết quả, ghi nhận làm minh chứng kỹ năng.", ready: false },
];

/** Thực tập sinh của công ty: danh sách thật là các hồ sơ đã nhận việc (HIRED). */
export function HrInterns() {
  const allowed = useAuthGuard({ roles: ["HR"] });
  const userId = useAuthStore(state => state.user?.id);
  const hired = useQuery({
    queryKey: ["hr-interns", userId ?? null], enabled: allowed,
    queryFn: ({ signal }) => hrApplicationService.listHr({ status: "HIRED", page: 1, size: 50 }, signal),
  });
  if (!allowed) return null;
  const items = hired.data?.items ?? [];

  return <div className="hr-page hrd">
    <TaskBanner eyebrow="Thực tập & nhận việc" title="Thực tập sinh" illustration="interns"
      description={hired.data?.total != null ? `${hired.data.total} ứng viên đã nhận việc tại công ty. Theo dõi người mới và chuẩn bị cho kỳ nghiệm thu.` : "Danh sách ứng viên đã nhận việc tại công ty."}>
      <Link href="/hr/recruitment?status=OFFER"><UserSearch className="size-4" aria-hidden="true" />Hồ sơ đang chờ offer</Link>
    </TaskBanner>

    <section className="hrd-panel" aria-labelledby="hri-list">
      <div className="hrd-panel-head"><h2 id="hri-list"><GraduationCap aria-hidden="true" />Đã nhận việc</h2>
        <Link href="/hr/recruitment?status=HIRED" className="hrd-link">Mở trong hồ sơ ứng tuyển<ChevronRight aria-hidden="true" /></Link></div>
      <div className="hrd-panel-body">
        {hired.isPending ? <div className="my-3 h-32 rounded-xl bg-muted motion-safe:animate-pulse" />
          : hired.error ? <p className="hrd-empty" role="alert">{getErrorMessage(hired.error)}</p>
          : !items.length ? <p className="hrd-empty"><Inbox aria-hidden="true" />Chưa có ứng viên nào nhận việc. Khi ứng viên chấp nhận offer, họ sẽ xuất hiện ở đây.</p>
          : <ul className="hrd-list">{items.map(item => <li key={item.id}><Link href={`/hr/recruitment/${item.id}`} className="hrd-attention">
              <span className="hrd-avatar" aria-hidden="true">{initials(item.candidateName)}</span>
              <span className="min-w-0 flex-1"><strong>{item.candidateName}</strong><small>{item.jobTitle} · nhận việc {dateFmt.format(new Date(item.updatedAt))}</small></span>
              <span className="hrd-hired">Đã nhận việc</span>
              <ChevronRight className="size-4 shrink-0 text-[var(--text-muted)]" aria-hidden="true" />
            </Link></li>)}</ul>}
      </div>
    </section>

    <section className="hrd-panel" aria-labelledby="hri-flow">
      <div className="hrd-panel-head"><h2 id="hri-flow"><ClipboardCheck aria-hidden="true" />Sau khi nhận việc</h2></div>
      <ol className="hrd-flow">{STEPS.map(step => <li key={step.title} data-ready={step.ready || undefined}>
        <span className="hrd-icon"><step.icon aria-hidden="true" /></span>
        <div><strong>{step.title}</strong><p>{step.text}</p>{!step.ready && <small>Sắp ra mắt</small>}</div>
      </li>)}</ol>
    </section>
  </div>;
}
