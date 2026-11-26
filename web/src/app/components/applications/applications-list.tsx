"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useRouter, useSearchParams } from "next/navigation";
import { BriefcaseBusiness } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { useAuthStore } from "@/hooks/use-auth-store";
import { APPLICATION_STATUS, applicationLinks, isApplicationStatus } from "@/config/application-status";
import { applicationService } from "@/services/application.service";
import { getErrorMessage } from "@/services/api";
import { ApplicationCard } from "./application-card";
import { listPage } from "./journey-navigation";
import "./candidate-journey.css";
import { TaskBanner } from "@/app/components/ui/task-banner";

export function ApplicationsList() {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const user = useAuthStore(s => s.user), token = useAuthStore(s => s.token);
  const router = useRouter(), params = useSearchParams();
  const raw = params.get("status"), status = isApplicationStatus(raw) ? raw : undefined;
  const page = listPage(params.get("page"));
  const query = useQuery({ queryKey: ["candidate-applications", user?.id, token, status, page], queryFn: () => applicationService.list({ status, page, size: 20 }), enabled: allowed, staleTime: 0, refetchOnMount: "always" });
  function url(nextStatus: string, nextPage: number) {
    const q = new URLSearchParams();
    if (nextStatus) q.set("status", nextStatus);
    if (nextPage > 1) q.set("page", String(nextPage));
    return `/candidate/applications${q.size ? `?${q}` : ""}`;
  }
  if (!allowed) return null;
  const data = !query.isFetching && !query.isError ? query.data : undefined;
  return <div className="journey">
    <TaskBanner title="Ứng tuyển của tôi" eyebrow="Hành trình nghề nghiệp" description="Mỗi hồ sơ, một cơ hội mới. Theo dõi phản hồi và bước tiếp theo trong hành trình ứng tuyển." illustration="applications">
      <Link href={applicationLinks.jobs}>Tìm việc làm mới <span aria-hidden="true">↗</span></Link>
    </TaskBanner>
    <div className="journey-toolbar"><div><label htmlFor="application-filter">Lọc theo trạng thái</label><select id="application-filter" value={status ?? ""} onChange={e => router.push(url(e.target.value, 1))}><option value="">Tất cả trạng thái</option>{Object.entries(APPLICATION_STATUS).map(([key, value]) => <option key={key} value={key}>{value.label}</option>)}</select></div>{data && <p className="journey-muted">{data.total} đơn ứng tuyển</p>}</div>
    {query.isFetching && <div className="journey-card" role="status" aria-busy="true">Đang tải đơn ứng tuyển…</div>}
    {query.isError && <div className="journey-error" role="alert"><p>{getErrorMessage(query.error)}</p><Button variant="outline" onClick={() => query.refetch()}>Thử lại</Button></div>}
    {data && (data.items.length ? <div className="journey-list">{data.items.map(a => <ApplicationCard key={a.id} application={a} returnTo={url(status ?? "", page)} />)}</div> : <section className="journey-card journey-empty"><BriefcaseBusiness aria-hidden="true" /><h2>Chưa có đơn ứng tuyển phù hợp</h2><p>{status ? "Thử chọn trạng thái khác để xem các đơn của bạn." : "Khám phá việc làm và gửi CV để bắt đầu hành trình mới."}</p>{status ? <Button variant="outline" onClick={() => router.push(url("", 1))}>Xoá bộ lọc</Button> : <Link href={applicationLinks.jobs} className="journey-link">Khám phá việc làm</Link>}</section>)}
    {data && data.total > data.size && <div className="journey-pagination"><span className="journey-muted">{data.total} đơn · Trang {page}</span><div className="journey-actions"><Button variant="outline" disabled={page <= 1} onClick={() => router.push(url(status ?? "", page - 1))}>Trước</Button><Button variant="outline" disabled={page * data.size >= data.total} onClick={() => router.push(url(status ?? "", page + 1))}>Sau</Button></div></div>}
  </div>;
}
