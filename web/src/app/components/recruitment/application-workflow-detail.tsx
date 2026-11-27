"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { Button } from "@/app/components/ui/button";
import { ApplicationStatusBadge } from "@/app/components/applications/application-status-badge";
import { APPLICATION_STATUS, formatApplicationDate } from "@/config/application-status";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { useAuthStore } from "@/hooks/use-auth-store";
import type { ApplicationStatus } from "@/interface/application";
import type { HrApplicationDetail, MatchLookupResponse } from "@/interface/hr-application";
import { ApiError, getErrorMessage } from "@/services/api";
import { hrApplicationService } from "@/services/hr-application.service";
import { InterviewRounds } from "./interview-rounds";
import { MatchCard } from "./match-card";
import { OfferPanel } from "./offer-panel";
import { HrApplicationDetailView } from "./hr-application-detail-view";

export function ApplicationWorkflowDetail({ id, role }: { id: string; role: "HR" | "STAFF" | "ADMIN" }) {
  const allowed = useAuthGuard({ roles: [role] });
  const viewerId = useAuthStore(state => state.user?.id), token = useAuthStore(state => state.token);
  const matchGeneration = useRef(0);
  const [snapshot, setSnapshot] = useState<{ application: HrApplicationDetail; viewerId: string | undefined; token: string | null; matchRevision: number } | null>(null);
  const application = snapshot?.viewerId === viewerId && snapshot?.token === token && snapshot?.application.id === id ? snapshot.application : null;
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
    const controller = new AbortController();
    const generation = ++matchGeneration.current;
    hrApplicationService.detail(id, controller.signal).then(value => { if (active) {
      setSnapshot(current => {
        const preserveMatch = generation !== matchGeneration.current && current?.application.id === id && current.viewerId === viewerId && current.token === token;
        return { application: preserveMatch ? { ...value, match: current.application.match, matchStatus: current.application.matchStatus } : value,
          viewerId, token, matchRevision: preserveMatch ? current.matchRevision : generation };
      }); setError("");
    } })
      .catch(reason => { if (active) setError(getErrorMessage(reason)); });
    return () => { active = false; controller.abort(); };
  }, [allowed, id, version, viewerId, token]);
  function updateMatch(value: MatchLookupResponse, generation: number) {
    setSnapshot(current => generation === matchGeneration.current && current?.application.id === id && current.viewerId === viewerId && current.token === token
      ? { ...current, matchRevision: generation, application: { ...current.application, match: value.match, matchStatus: value.matchStatus } } : current);
  }
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
  const errors = <>
    {error && <div role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm">{error}<Button variant="outline" className="ml-3" onClick={reload}>Tải lại</Button></div>}
    {mutationError && <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm">{mutationError}</p>}
  </>;
  if (!application) return <div className="space-y-4">{errors}{!error && <p role="status">Đang tải chi tiết đơn…</p>}</div>;
  const summary = (<section className="ptit-surface p-6 sm:p-7"><div className="flex flex-wrap items-start justify-between gap-3">
        <div><p className="ptit-eyebrow">{application.companyName}</p><h1 className="mt-1 text-2xl font-bold text-heading">{application.jobTitle}</h1>
          <p className="mt-2 text-sm text-body">Ứng viên: <strong className="text-heading">{application.candidate.fullName}</strong> · {application.candidate.email}</p>
          {application.candidate.phone && <p className="mt-1 text-sm text-body">Điện thoại: {application.candidate.phone}</p>}
          <p className="mt-1.5 text-xs text-muted-foreground">Nộp ngày {formatApplicationDate(application.createdAt)}</p></div>
        <ApplicationStatusBadge status={application.status} /></div>
        <Button variant="outline" className="mt-5" disabled={downloading} onClick={downloadCv}>{downloading ? "Đang tải…" : "Tải CV đã nộp"}</Button>
        {application.coverLetter && <div className="mt-5 border-t border-border pt-4"><h2 className="font-bold text-heading">Thư giới thiệu</h2><p className="mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed text-body">{application.coverLetter}</p></div>}
      </section>);
  const matchCard = (role !== "ADMIN" && <MatchCard key={`${viewerId}:${id}`} applicationId={id} match={application.match} matchStatus={application.matchStatus}
        revision={snapshot?.matchRevision ?? 0} onRequestStart={() => ++matchGeneration.current}
        isRequestCurrent={generation => generation === matchGeneration.current} onMatchChanged={updateMatch} />);
  const transitionPanel = (role === "HR" && application.allowedTransitions.length > 0 && <section className="ptit-surface p-5"><h2 className="font-bold text-heading">Chuyển bước</h2>
        <label className="mt-3 block text-sm font-semibold text-heading">Ghi chú nội bộ<textarea aria-label="Ghi chú chuyển bước" value={note} onChange={e => setNote(e.target.value)} maxLength={1000}
          className="mt-1.5 w-full rounded-lg border border-input bg-white px-3 py-2 font-normal" /></label>
        <div className="mt-3 flex flex-wrap gap-2">{application.allowedTransitions.map(target => <Button key={target} variant={target === "REJECTED" ? "outline" : "default"}
          disabled={busy} onClick={() => transition(target)}>Chuyển sang {APPLICATION_STATUS[target].label}</Button>)}</div>
      </section>);
  const rounds = (<InterviewRounds key={application.id} application={application} readOnly={role !== "HR"} onChanged={reload} />);
  const offer = role === "HR" ? (<OfferPanel key={application.id + application.status} application={application} onChanged={reload} />) : null;
  const history = (<section className="ptit-surface p-6"><h2 className="text-lg font-bold text-heading">Lịch sử trạng thái</h2>
        <ol className="mt-4 space-y-4 border-l-2 border-[#FECDD3] pl-5">{application.history.map(item => <li key={item.id} className="relative text-sm before:absolute before:-left-[25px] before:top-1.5 before:h-2 before:w-2 before:rounded-full before:bg-primary">
          <strong className="text-heading">{APPLICATION_STATUS[item.toStatus].label}</strong><span className="ml-2 text-xs text-muted-foreground">{formatApplicationDate(item.createdAt)}</span>
          {item.note && <p className="mt-1 whitespace-pre-wrap text-body">{item.note}</p>}</li>)}</ol>
      </section>);
  if (role === "HR") return <HrApplicationDetailView key={id} application={application}
    errors={errors} matchCard={matchCard} transitionPanel={transitionPanel} rounds={rounds} offer={offer} history={history}
    downloading={downloading} onDownloadCv={downloadCv} />;
  return <div className="ptit-page max-w-5xl">
    <Link href={root} className="inline-flex w-fit items-center text-sm font-semibold text-primary underline-offset-4 hover:underline">← Danh sách đơn ứng tuyển</Link>
    {errors}{summary}{matchCard}{rounds}{history}
  </div>;
}
