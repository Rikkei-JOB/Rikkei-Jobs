"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { BriefcaseBusiness, ArrowLeft } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { APPLICATION_STATUS, isApplicationStatus } from "@/config/application-status";
import type { ApplicationPage } from "@/interface/application";
import { applicationService } from "@/services/application.service";
import { getErrorMessage } from "@/services/api";
import { ApplicationCard } from "./application-card";

export function ApplicationsList() {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const router = useRouter();
  const params = useSearchParams();
  const rawStatus = params.get("status");
  const status = isApplicationStatus(rawStatus) ? rawStatus : undefined;
  const rawPage = Number(params.get("page") ?? "1");
  const page = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1;
  const [data, setData] = useState<ApplicationPage | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);
  useEffect(() => {
    if (!allowed) return;
    let active = true;
    applicationService.list({ status, page, size: 20 }).then(value => { if (active) { setData(value); setError(""); } })
      .catch(err => { if (active) setError(getErrorMessage(err)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [allowed, status, page, version]);

  function navigate(nextStatus: string, nextPage: number) {
    const query = new URLSearchParams();
    if (nextStatus) query.set("status", nextStatus);
    if (nextPage > 1) query.set("page", String(nextPage));
    setLoading(true); router.push(`/candidate/applications${query.size ? `?${query}` : ""}`);
  }
  if (!allowed) return null;
  return <main className="min-h-screen bg-muted/20 px-4 py-8 sm:px-6">
    <div className="mx-auto max-w-5xl">
      <Link href="/candidate" className="mb-6 inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-primary"><ArrowLeft className="h-4 w-4" />Hồ sơ của tôi</Link>
      <div className="mb-8 flex flex-wrap items-end justify-between gap-5">
        <div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Hành trình nghề nghiệp</p><h1 className="text-3xl font-semibold tracking-tight">Ứng tuyển của tôi</h1><p className="mt-2 text-sm text-muted-foreground">Theo dõi các cơ hội và tiến trình tuyển dụng của bạn.</p></div>
        <div className="space-y-2"><label htmlFor="application-filter" className="block text-xs font-medium">Lọc theo trạng thái</label>
          <select id="application-filter" value={status ?? ""} onChange={e => navigate(e.target.value, 1)} className="h-10 rounded-lg border bg-background px-3 text-sm"><option value="">Tất cả trạng thái</option>
            {Object.entries(APPLICATION_STATUS).map(([key, value]) => <option key={key} value={key}>{value.label}</option>)}
          </select>
        </div>
      </div>
      {error && <div role="alert" className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-destructive/20 bg-destructive/5 p-4 text-sm"><p>{error}</p><Button variant="outline" onClick={() => { setLoading(true); setVersion(v => v + 1); }}>Thử lại</Button></div>}
      {loading && <p role="status" className="mb-5 text-sm text-muted-foreground">Đang tải đơn ứng tuyển…</p>}
      {data && !loading && data.items.length === 0 ? <section className="rounded-xl border border-dashed bg-background px-6 py-16 text-center">
        <BriefcaseBusiness className="mx-auto mb-4 h-10 w-10 text-muted-foreground" /><h2 className="text-lg font-semibold">Chưa có đơn ứng tuyển phù hợp</h2>
        <p className="mt-2 text-sm text-muted-foreground">{status ? "Thử chọn trạng thái khác để xem các đơn của bạn." : "Khám phá việc làm và gửi CV để bắt đầu hành trình mới."}</p>
        <Link href="/jobs" className="mt-5 inline-block text-sm font-medium text-primary underline">Khám phá việc làm</Link>
      </section> : <div className="space-y-4" aria-busy={loading}>{data?.items.map(a => <ApplicationCard key={a.id} application={a} />)}</div>}
      {data && <div className="mt-6 flex items-center justify-between gap-3 text-sm"><span className="text-muted-foreground">{data.total} đơn · Trang {page}</span>
        <div className="flex gap-2"><Button variant="outline" disabled={loading || page <= 1} onClick={() => navigate(status ?? "", page - 1)}>Trước</Button><Button variant="outline" disabled={loading || page * data.size >= data.total} onClick={() => navigate(status ?? "", page + 1)}>Sau</Button></div>
      </div>}
    </div>
  </main>;
}
