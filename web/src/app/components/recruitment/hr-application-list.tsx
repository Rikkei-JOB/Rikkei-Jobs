"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { ArrowUpRight, Inbox } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { ApplicationStatusBadge } from "@/app/components/applications/application-status-badge";
import { APPLICATION_STATUS, formatApplicationDate, isApplicationStatus } from "@/config/application-status";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { useAuthStore } from "@/hooks/use-auth-store";
import { hrApplicationService } from "@/services/hr-application.service";
import { getErrorMessage } from "@/services/api";
import { HrJobFilter } from "./hr-job-filter";

export function HrApplicationList() {
  const allowed = useAuthGuard({ roles: ["HR"] });
  const viewerId = useAuthStore(state => state.user?.id), token = useAuthStore(state => state.token);
  const search = useSearchParams();
  const router = useRouter();
  const jobId = search.get("jobId") ?? "";
  const rawStatus = search.get("status");
  const status = isApplicationStatus(rawStatus) ? rawStatus : undefined;
  const rawPage = Number(search.get("page") ?? 1);
  const page = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1;
  // Workflow actions can change status and filter membership while this list is unmounted.
  const query = useQuery({ queryKey: ["hr-applications", viewerId, token, jobId, status, page], queryFn: ({ signal }) => hrApplicationService.listHr({ jobId: jobId || undefined, status, page, size: 20 }, signal), enabled: allowed, staleTime: 0, refetchOnMount: "always" });
  function navigate(changes: Record<string, string>) {
    const next = new URLSearchParams(search.toString());
    for (const [key, value] of Object.entries(changes)) { if (value) next.set(key, value); else next.delete(key); }
    router.push(`/hr/recruitment${next.size ? `?${next}` : ""}`, { scroll: false });
  }
  if (!allowed) return null;
  const filtered = Boolean(jobId || status);
  const listUrl = `/hr/recruitment${search.size ? `?${search}` : ""}`;
  const href = (id: string) => `/hr/recruitment/${id}?${new URLSearchParams({ returnTo: listUrl })}`;
  const data = query.isError || query.isFetching ? undefined : query.data;
  return <div className="hr-page">
    <header className="hr-page-heading"><div><p className="hr-eyebrow">QUẢN LÝ TUYỂN DỤNG</p><h1>Hồ sơ ứng tuyển</h1><p className="hr-description">Tìm hồ sơ phù hợp và theo dõi từng bước tuyển dụng của công ty.</p></div></header>
    <section className="hr-surface hr-filter-bar" aria-label="Bộ lọc hồ sơ">
      <HrJobFilter value={jobId} onChange={value => navigate({ jobId: value, page: "" })} />
      <div className="min-w-0 flex-1"><label htmlFor="hr-status-filter">Trạng thái</label><select id="hr-status-filter" value={status ?? ""} onChange={event => navigate({ status: event.target.value, page: "" })}>
        <option value="">Tất cả trạng thái</option>{Object.entries(APPLICATION_STATUS).map(([value, info]) => <option key={value} value={value}>{info.label}</option>)}
      </select></div>
      {filtered && <Button variant="outline" onClick={() => router.push("/hr/recruitment", { scroll: false })}>Xoá bộ lọc</Button>}
    </section>
    {(query.isPending || query.isFetching) && <div role="status" aria-label="Đang tải hồ sơ" className="hr-surface space-y-4"><p>Đang tải hồ sơ…</p>{[1, 2, 3].map(row => <div key={row} className="h-12 rounded-lg bg-muted motion-safe:animate-pulse" />)}</div>}
    {query.isError && <div role="alert" className="hr-surface"><p>{getErrorMessage(query.error)}</p><Button variant="outline" className="mt-3" onClick={() => query.refetch()}>Thử lại</Button></div>}
    {data && <>
      <div className="hr-results" role="status"><p><strong className="text-foreground">{data.total}</strong> hồ sơ{filtered ? " phù hợp bộ lọc" : " ứng tuyển"}</p>{filtered && <span>Đang lọc: {[jobId && "Vị trí đã chọn", status && APPLICATION_STATUS[status].label].filter(Boolean).join(" · ")}</span>}</div>
      {data.items.length === 0 ? <div className="hr-surface py-12 text-center"><Inbox className="mx-auto mb-4 size-9 text-primary" aria-hidden="true" /><h2 className="text-xl font-semibold">Chưa có hồ sơ phù hợp</h2><p className="hr-description">{filtered ? "Thử bỏ bộ lọc để xem các hồ sơ khác." : "Hồ sơ sẽ xuất hiện tại đây khi ứng viên nộp đơn vào tin tuyển dụng."}</p></div> : <>
        <div className="hr-table-container hr-desktop-applications"><table className="hr-table"><caption className="sr-only">Danh sách hồ sơ ứng tuyển</caption><thead><tr>{["Ứng viên", "Vị trí", "Ngày ứng tuyển", "Trạng thái", ""].map((title, index) => <th scope="col" key={index}>{title || <span className="sr-only">Thao tác</span>}</th>)}</tr></thead><tbody>{data.items.map(item => <tr key={item.id}>
          <td className="font-semibold text-heading">{item.candidateName}</td><td>{item.jobTitle}</td><td className="text-sm text-muted-foreground">{formatApplicationDate(item.createdAt)}</td><td><ApplicationStatusBadge status={item.status} /></td><td><Link href={href(item.id)} aria-label={`Xem hồ sơ ${item.candidateName}`} className="gap-1 whitespace-nowrap">Xem hồ sơ<ArrowUpRight size={16} aria-hidden="true" /></Link></td>
        </tr>)}</tbody></table></div>
        <div className="hr-mobile-applications space-y-4">{data.items.map(item => <article key={item.id} className="hr-surface break-words"><div className="flex flex-wrap items-start justify-between gap-2"><h2 className="text-lg font-semibold">{item.candidateName}</h2><ApplicationStatusBadge status={item.status} /></div><p className="mt-3">{item.jobTitle}</p><p className="mt-1 text-xs text-muted-foreground">Nộp ngày {formatApplicationDate(item.createdAt)}</p><Link href={href(item.id)} className="mt-3 inline-flex min-h-11 items-center gap-2 font-semibold text-primary" aria-label={`Xem hồ sơ ${item.candidateName}`}>Xem hồ sơ<ArrowUpRight size={16} aria-hidden="true" /></Link></article>)}</div>
      </>}
      <nav className="hr-results" aria-label="Phân trang hồ sơ"><span>Trang {page} / {Math.max(1, Math.ceil(data.total / data.size))}</span><div className="flex gap-2"><Button variant="outline" disabled={query.isFetching || page <= 1} onClick={() => navigate({ page: String(page - 1) })}>Trước</Button><Button variant="outline" disabled={query.isFetching || page * data.size >= data.total} onClick={() => navigate({ page: String(page + 1) })}>Sau</Button></div></nav>
    </>}
  </div>;
}
