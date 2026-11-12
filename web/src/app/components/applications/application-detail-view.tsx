"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ArrowLeft, Building2, Download } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { applicationLinks, formatApplicationDate } from "@/config/application-status";
import type { ApplicationDetail } from "@/interface/application";
import { applicationService } from "@/services/application.service";
import { getErrorMessage } from "@/services/api";
import { ApplicationStatusBadge } from "./application-status-badge";
import { ApplicationProgress } from "./application-progress";
import { ApplicationHistory } from "./application-history";
import { WithdrawDialog } from "./withdraw-dialog";

export function ApplicationDetailView({ id }: { id: string }) {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const [application, setApplication] = useState<ApplicationDetail | null>(null);
  const [error, setError] = useState("");
  const [version, setVersion] = useState(0);
  const [downloading, setDownloading] = useState(false);
  useEffect(() => {
    if (!allowed) return;
    let active = true;
    applicationService.detail(id).then(value => { if (active) { setApplication(value); setError(""); } })
      .catch(err => { if (active) setError(getErrorMessage(err)); });
    return () => { active = false; };
  }, [allowed, id, version]);
  async function download() {
    setDownloading(true);
    try {
      const blob = await applicationService.downloadSnapshot(id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url; link.download = `cv-da-nop-${id}.pdf`; link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (err) { setError(getErrorMessage(err)); }
    finally { setDownloading(false); }
  }
  if (!allowed) return null;
  return <main className="min-h-screen bg-muted/20 px-4 py-8 sm:px-6"><div className="mx-auto max-w-5xl">
    <Link href="/candidate/applications" className="mb-6 inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-primary"><ArrowLeft className="h-4 w-4" />Ứng tuyển của tôi</Link>
    {error && <div role="alert" className="mb-5 rounded-lg border border-destructive/20 bg-destructive/5 p-4 text-sm"><p>{error}</p><Button variant="outline" className="mt-3" onClick={() => setVersion(v => v + 1)}>Tải lại</Button></div>}
    {!application && !error && <p role="status">Đang tải chi tiết đơn…</p>}
    {application && <div className="space-y-6">
      <section className="rounded-xl border bg-background p-5 sm:p-7">
        <div className="flex flex-wrap items-start justify-between gap-4"><div><h1 className="text-2xl font-semibold sm:text-3xl">{application.jobTitle}</h1><p className="mt-3 flex items-center gap-2 text-sm text-muted-foreground"><Building2 className="h-4 w-4" />{application.companyName}</p></div><ApplicationStatusBadge status={application.status} /></div>
        <p className="mt-4 text-sm text-muted-foreground">Ngày nộp: {formatApplicationDate(application.createdAt)}</p>
        <div className="mt-6 flex flex-wrap items-center gap-3">
          <Button variant="outline" className="h-10 px-4" onClick={download} disabled={downloading}><Download className="mr-2 h-4 w-4" />{downloading ? "Đang tải…" : "Tải CV đã nộp"}</Button>
          {application.canWithdraw && <WithdrawDialog id={id} onWithdrawn={setApplication} onConflict={() => setVersion(v => v + 1)} />}
          {(application.status === "REJECTED" || application.status === "WITHDRAWN") && <Link href={applicationLinks.job(application.jobId)} className="rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground">Ứng tuyển lại</Link>}
          <Link href={applicationLinks.job(application.jobId)} className="text-sm text-primary underline">Xem tin tuyển dụng</Link>
        </div>
        <p className="mt-4 text-xs text-muted-foreground">Bản CV đã nộp được giữ nguyên, kể cả khi bạn chỉnh sửa CV sau đó.</p>
      </section>
      <ApplicationProgress status={application.status} history={application.history} />
      {application.coverLetter && <section className="rounded-xl border bg-background p-5 sm:p-6"><h2 className="mb-3 font-semibold">Thư giới thiệu đã gửi</h2><p className="whitespace-pre-wrap break-words text-sm leading-relaxed">{application.coverLetter}</p></section>}
      <ApplicationHistory items={application.history} />
    </div>}
  </div></main>;
}
