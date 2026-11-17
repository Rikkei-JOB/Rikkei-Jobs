"use client";

import { useEffect, useState } from "react";
import { Button } from "@/app/components/ui/button";
import { formatApplicationDate } from "@/config/application-status";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import type { InterviewRound, MentorRoundPage, RoundResult } from "@/interface/hr-application";
import { ApiError, getErrorMessage } from "@/services/api";
import { hrApplicationService } from "@/services/hr-application.service";

function MentorRoundEditor({ round, onSaved }: { round: InterviewRound; onSaved: () => void }) {
  const [result, setResult] = useState<RoundResult>(round.result);
  const [score, setScore] = useState(round.score?.toString() ?? "");
  const [note, setNote] = useState(round.note ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function save() {
    setBusy(true); setError("");
    try {
      await hrApplicationService.updateMentorRound(round.id, {
        result, score: result === "PENDING" || score === "" ? null : Number(score), note: note || null,
      });
      onSaved();
    } catch (reason) {
      setError(getErrorMessage(reason));
      if (reason instanceof ApiError && reason.status === 409) onSaved();
    } finally { setBusy(false); }
  }
  return <div className="mt-4 grid gap-3 border-t pt-4 sm:grid-cols-3">
    <label className="text-sm">Kết quả<select aria-label={`Kết quả vòng ${round.roundNo}`} value={result} onChange={event => setResult(event.target.value as RoundResult)} className="mt-1 w-full rounded-lg border bg-background px-3 py-2">
      <option value="PENDING">Chờ kết quả</option><option value="PASS">Đạt</option><option value="FAIL">Không đạt</option></select></label>
    <label className="text-sm">Điểm<input aria-label={`Điểm vòng ${round.roundNo}`} type="number" min="0" max="100" step="0.01" disabled={result === "PENDING"} value={score} onChange={event => setScore(event.target.value)} className="mt-1 w-full rounded-lg border bg-background px-3 py-2" /></label>
    <label className="text-sm sm:col-span-3">Ghi chú<textarea aria-label={`Ghi chú vòng ${round.roundNo}`} value={note} onChange={event => setNote(event.target.value)} maxLength={2000} className="mt-1 w-full rounded-lg border bg-background px-3 py-2" /></label>
    {error && <p role="alert" className="text-sm text-destructive sm:col-span-3">{error}</p>}
    <Button disabled={busy} onClick={save} className="sm:w-fit">{busy ? "Đang lưu…" : "Lưu kết quả"}</Button>
  </div>;
}

export default function MentorReviewsPage() {
  const allowed = useAuthGuard({ roles: ["MENTOR"] });
  const [data, setData] = useState<MentorRoundPage | null>(null);
  const [error, setError] = useState("");
  const [page, setPage] = useState(1);
  const [version, setVersion] = useState(0);
  useEffect(() => {
    if (!allowed) return;
    let active = true;
    hrApplicationService.listMentorRounds({ page, size: 20 })
      .then(value => { if (active) { setData(value); setError(""); } })
      .catch(reason => { if (active) setError(getErrorMessage(reason)); });
    return () => { active = false; };
  }, [allowed, page, version]);
  if (!allowed) return null;
  return <main className="min-h-screen bg-muted/20 px-4 py-8 sm:px-6"><div className="mx-auto max-w-4xl space-y-5">
    <header><h1 className="text-3xl font-semibold">Vòng phỏng vấn được mời</h1>
      <p className="mt-2 text-sm text-muted-foreground">Ghi kết quả cho những vòng được giao cho bạn.</p></header>
    {error && <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm">{error}</p>}
    {!data && !error && <p role="status">Đang tải vòng phỏng vấn…</p>}
    {data?.items.length === 0 && <p className="rounded-xl border bg-background p-6 text-sm text-muted-foreground">Chưa có vòng phỏng vấn được mời.</p>}
    {data?.items.map(item => <section key={item.round.id} className="rounded-xl border bg-background p-5">
      <div className="flex flex-wrap items-start justify-between gap-2"><div><p className="text-sm text-muted-foreground">{item.companyName}</p>
        <h2 className="text-xl font-semibold">{item.jobTitle}</h2></div><span className="text-sm">{item.applicationStatus}</span></div>
      <h3 className="mt-4 font-medium">Vòng {item.round.roundNo}: {item.round.name}</h3>
      {item.round.scheduledAt && <p className="mt-1 text-sm text-muted-foreground">Lịch: {formatApplicationDate(item.round.scheduledAt)}</p>}
      <p className="mt-2 text-sm">Kết quả: {item.round.result}</p>
      {item.round.score !== null && <p className="mt-2 text-sm">Điểm: {item.round.score}/100</p>}
      {item.round.note && <p className="mt-2 whitespace-pre-wrap text-sm">{item.round.note}</p>}
      {item.applicationStatus === "INTERVIEW" && <MentorRoundEditor key={`${item.round.id}-${item.round.result}-${item.round.score}`} round={item.round} onSaved={() => setVersion(value => value + 1)} />}
    </section>)}
    {data && <nav className="flex items-center justify-between text-sm"><span>{data.total} vòng · Trang {page}</span><div className="flex gap-2">
      <Button variant="outline" disabled={page <= 1} onClick={() => setPage(value => value - 1)}>Trước</Button>
      <Button variant="outline" disabled={page * data.size >= data.total} onClick={() => setPage(value => value + 1)}>Sau</Button>
    </div></nav>}
  </div></main>;
}
