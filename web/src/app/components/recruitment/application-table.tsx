"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Button } from "@/app/components/ui/button";
import { ApplicationStatusBadge } from "@/app/components/applications/application-status-badge";
import { APPLICATION_STATUS, formatApplicationDate, isApplicationStatus } from "@/config/application-status";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import type { HrApplicationPage } from "@/interface/hr-application";
import { hrApplicationService } from "@/services/hr-application.service";
import { getErrorMessage } from "@/services/api";

export function ApplicationTable({ role }: { role: "HR" | "STAFF" | "ADMIN" }) {
  const allowed = useAuthGuard({ roles: [role] });
  const router = useRouter();
  const search = useSearchParams();
  const status = isApplicationStatus(search.get("status")) ? search.get("status") as keyof typeof APPLICATION_STATUS : undefined;
  const rawPage = Number(search.get("page") ?? "1");
  const page = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1;
  const companyId = search.get("companyId") ?? "";
  const jobId = search.get("jobId") ?? "";
  const [data, setData] = useState<HrApplicationPage | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const root = role === "HR" ? "/hr/recruitment" : role === "ADMIN" ? "/admin/applications" : "/staff/applications";

  useEffect(() => {
    if (!allowed) return;
    let active = true;
    const params = { companyId: companyId || undefined, jobId: jobId || undefined, status, page, size: 20 };
    (role === "HR" ? hrApplicationService.listHr(params) : hrApplicationService.listStaff(params))
      .then(value => { if (active) { setData(value); setError(""); } })
      .catch(reason => { if (active) setError(getErrorMessage(reason)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [allowed, role, companyId, jobId, status, page]);

  function navigate(changes: Record<string, string>) {
    const next = new URLSearchParams(search.toString());
    for (const [key, value] of Object.entries(changes)) {
      if (value) next.set(key, value);
      else next.delete(key);
    }
    setLoading(true);
    router.push(`${root}${next.size ? `?${next}` : ""}`);
  }
  if (!allowed) return null;
  return <main className="min-h-screen bg-muted/20 px-4 py-8 sm:px-6"><div className="mx-auto max-w-6xl space-y-6">
    <header><h1 className="text-3xl font-semibold">{role === "HR" ? "Đơn ứng tuyển" : "Toàn bộ đơn ứng tuyển"}</h1>
      <p className="mt-2 text-sm text-muted-foreground">Theo dõi tiến trình tuyển dụng và mở từng đơn để xem chi tiết.</p></header>
    <section className="grid gap-3 rounded-xl border bg-background p-4 sm:grid-cols-3">
      {role !== "HR" && <label className="text-sm">Mã công ty<input aria-label="Mã công ty" defaultValue={companyId} key={`company-${companyId}`}
        onBlur={event => navigate({ companyId: event.target.value.trim(), page: "" })}
        onKeyDown={event => { if (event.key === "Enter") event.currentTarget.blur(); }}
        className="mt-1 w-full rounded-lg border bg-background px-3 py-2" placeholder="UUID công ty" /></label>}
      <label className="text-sm">Mã JD<input aria-label="Mã JD" defaultValue={jobId} key={`job-${jobId}`}
        onBlur={event => navigate({ jobId: event.target.value.trim(), page: "" })}
        onKeyDown={event => { if (event.key === "Enter") event.currentTarget.blur(); }}
        className="mt-1 w-full rounded-lg border bg-background px-3 py-2" placeholder="UUID JD" /></label>
      <label className="text-sm">Trạng thái<select aria-label="Trạng thái" value={status ?? ""}
        onChange={event => navigate({ status: event.target.value, page: "" })}
        className="mt-1 w-full rounded-lg border bg-background px-3 py-2"><option value="">Tất cả</option>
        {Object.entries(APPLICATION_STATUS).map(([value, info]) => <option key={value} value={value}>{info.label}</option>)}
      </select></label>
    </section>
    {error && <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm">{error}</p>}
    {loading && <p role="status">Đang tải đơn ứng tuyển…</p>}
    {data && !loading && <section className="overflow-hidden rounded-xl border bg-background">
      {data.items.length === 0 ? <p className="p-8 text-center text-muted-foreground">Không có đơn phù hợp.</p>
        : <div className="divide-y">{data.items.map(item => <Link key={item.id} href={`${root}/${item.id}`}
          className="grid gap-2 p-4 hover:bg-muted/40 sm:grid-cols-[1.3fr_1.2fr_1fr_auto] sm:items-center sm:gap-4">
          <div><p className="font-medium">{item.candidateName}</p><p className="text-xs text-muted-foreground">{item.companyName}</p></div>
          <p className="text-sm">{item.jobTitle}</p><p className="text-xs text-muted-foreground">{formatApplicationDate(item.createdAt)}</p>
          <ApplicationStatusBadge status={item.status} /></Link>)}</div>}
    </section>}
    {data && <nav className="flex items-center justify-between text-sm"><span>{data.total} đơn · Trang {page}</span><div className="flex gap-2">
      <Button variant="outline" disabled={loading || page <= 1} onClick={() => navigate({ page: String(page - 1) })}>Trước</Button>
      <Button variant="outline" disabled={loading || page * data.size >= data.total} onClick={() => navigate({ page: String(page + 1) })}>Sau</Button>
    </div></nav>}
  </div></main>;
}
