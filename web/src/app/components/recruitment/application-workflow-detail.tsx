"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Button } from "@/app/components/ui/button";
import { ApplicationStatusBadge } from "@/app/components/applications/application-status-badge";
import { APPLICATION_STATUS, formatApplicationDate } from "@/config/application-status";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import type { ApplicationStatus } from "@/interface/application";
import type { HrApplicationDetail } from "@/interface/hr-application";
import { ApiError, getErrorMessage } from "@/services/api";
import { hrApplicationService } from "@/services/hr-application.service";
import { InterviewRounds } from "./interview-rounds";
import { MatchCard } from "./match-card";
import { OfferPanel } from "./offer-panel";

export function ApplicationWorkflowDetail({ id, role }: { id: string; role: "HR" | "STAFF" | "ADMIN" }) {
  const allowed = useAuthGuard({ roles: [role] });
  const [application, setApplication] = useState<HrApplicationDetail | null>(null);
  const [error, setError] = useState("");
  const [mutationError, setMutationError] = useState("");
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [version, setVersion] = useState(0);
  const reload = useCallback(() => setVersion(v => v + 1), []);
  useEffect(() => {
    if (!allowed) return;
    let active = true;
    hrApplicationService.detail(id).then(value => { if (active) { setApplication(value); setError(""); } })
      .catch(reason => { if (active) setError(getErrorMessage(reason)); });
    return () => { active = false; };
  }, [allowed, id, version]);
  async function transition(toStatus: ApplicationStatus) {
    setBusy(true); setMutationError("");
    try { await hrApplicationService.transition(id, toStatus, note); setNote(""); reload(); }
    catch (reason) {
      setMutationError(getErrorMessage(reason));
      if (reason instanceof ApiError && reason.status === 409) reload();
    } finally { setBusy(false); }
  }
  async function downloadCv() {
    setDownloading(true); setError("");
    try {
      const blob = await hrApplicationService.downloadCv(id);
      const url = URL.createObjectURL(blob); const link = document.createElement("a");
      link.href = url; link.download = `cv-da-nop-${id}.pdf`; link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (reason) { setError(getErrorMessage(reason)); }
    finally { setDownloading(false); }
  }
  if (!allowed) return null;
  const root = role === "HR" ? "/hr/recruitment" : role === "ADMIN" ? "/admin/applications" : "/staff/applications";
  return <main className="min-h-screen bg-muted/20 px-4 py-8 sm:px-6"><div className="mx-auto max-w-5xl space-y-5">
    <Link href={root} className="text-sm text-primary underline">← Danh sách đơn ứng tuyển</Link>
    {error && <div role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm">{error}<Button variant="outline" className="ml-3" onClick={reload}>Tải lại</Button></div>}
    {mutationError && <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm">{mutationError}</p>}
    {!application && !error && <p role="status">Đang tải chi tiết đơn…</p>}
    {application && <>
      <section className="rounded-xl border bg-background p-5 sm:p-7"><div className="flex flex-wrap items-start justify-between gap-3">
        <div><p className="text-sm text-muted-foreground">{application.companyName}</p><h1 className="mt-1 text-2xl font-semibold">{application.jobTitle}</h1>
          <p className="mt-2 text-sm">Ứng viên: <strong>{application.candidate.fullName}</strong> · {application.candidate.email}</p>
          {application.candidate.phone && <p className="mt-1 text-sm">Điện thoại: {application.candidate.phone}</p>}
          <p className="mt-1 text-xs text-muted-foreground">Nộp ngày {formatApplicationDate(application.createdAt)}</p></div>
        <ApplicationStatusBadge status={application.status} /></div>
        <Button variant="outline" className="mt-5" disabled={downloading} onClick={downloadCv}>{downloading ? "Đang tải…" : "Tải CV đã nộp"}</Button>
        {application.coverLetter && <div className="mt-5 border-t pt-4"><h2 className="font-medium">Thư giới thiệu</h2><p className="mt-2 whitespace-pre-wrap break-words text-sm">{application.coverLetter}</p></div>}
      </section>
      <MatchCard match={application.match} />
      {role === "HR" && application.allowedTransitions.length > 0 && <section className="rounded-xl border bg-background p-5"><h2 className="font-semibold">Chuyển bước</h2>
        <label className="mt-3 block text-sm">Ghi chú nội bộ<textarea aria-label="Ghi chú chuyển bước" value={note} onChange={e => setNote(e.target.value)} maxLength={1000}
          className="mt-1 w-full rounded-lg border bg-background px-3 py-2" /></label>
        <div className="mt-3 flex flex-wrap gap-2">{application.allowedTransitions.map(target => <Button key={target} variant={target === "REJECTED" ? "outline" : "default"}
          disabled={busy} onClick={() => transition(target)}>Chuyển sang {APPLICATION_STATUS[target].label}</Button>)}</div>
      </section>}
      <InterviewRounds application={application} readOnly={role !== "HR"} onChanged={reload} />
      {role === "HR" && <OfferPanel key={application.id + application.status} application={application} onChanged={reload} />}
      <section className="rounded-xl border bg-background p-5"><h2 className="text-lg font-semibold">Lịch sử trạng thái</h2>
        <ol className="mt-4 space-y-4 border-l pl-5">{application.history.map(item => <li key={item.id} className="relative text-sm before:absolute before:-left-[25px] before:top-1 before:h-2 before:w-2 before:rounded-full before:bg-primary">
          <strong>{APPLICATION_STATUS[item.toStatus].label}</strong><span className="ml-2 text-muted-foreground">{formatApplicationDate(item.createdAt)}</span>
          {item.note && <p className="mt-1 whitespace-pre-wrap text-muted-foreground">{item.note}</p>}</li>)}</ol>
      </section>
    </>}
  </div></main>;
}
