"use client";

import { useState } from "react";
import { Button } from "@/app/components/ui/button";
import { formatApplicationDate } from "@/config/application-status";
import type { HrApplicationDetail, InterviewRound, RoundResult } from "@/interface/hr-application";
import { getErrorMessage } from "@/services/api";
import { hrApplicationService } from "@/services/hr-application.service";

function RoundEditor({ applicationId, round, onSaved }: { applicationId: string; round: InterviewRound; onSaved: () => void }) {
  const [result, setResult] = useState<RoundResult>(round.result);
  const [score, setScore] = useState(round.score?.toString() ?? "");
  const [note, setNote] = useState(round.note ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function save() {
    setBusy(true); setError("");
    try {
      await hrApplicationService.updateRound(applicationId, round.id, {
        result, score: result === "PENDING" || score === "" ? null : Number(score), note: note || null,
      });
      onSaved();
    } catch (reason) { setError(getErrorMessage(reason)); }
    finally { setBusy(false); }
  }
  return <div className="mt-4 grid gap-3 border-t pt-4 sm:grid-cols-3">
    <label className="text-sm">Kết quả<select aria-label={`Kết quả vòng ${round.roundNo}`} value={result} onChange={e => setResult(e.target.value as RoundResult)} className="mt-1 w-full rounded-lg border bg-background px-3 py-2">
      <option value="PENDING">Chờ kết quả</option><option value="PASS">Đạt</option><option value="FAIL">Không đạt</option></select></label>
    <label className="text-sm">Điểm<input aria-label={`Điểm vòng ${round.roundNo}`} type="number" min="0" max="100" step="0.01" disabled={result === "PENDING"} value={score} onChange={e => setScore(e.target.value)} className="mt-1 w-full rounded-lg border bg-background px-3 py-2" /></label>
    <label className="text-sm sm:col-span-3">Ghi chú<textarea aria-label={`Ghi chú vòng ${round.roundNo}`} value={note} onChange={e => setNote(e.target.value)} maxLength={2000} className="mt-1 w-full rounded-lg border bg-background px-3 py-2" /></label>
    {error && <p role="alert" className="text-sm text-red-700 sm:col-span-3">{error}</p>}
    <Button className="sm:w-fit" disabled={busy} onClick={save}>{busy ? "Đang lưu…" : "Lưu kết quả"}</Button>
  </div>;
}

export function InterviewRounds({ application, readOnly, onChanged }: { application: HrApplicationDetail; readOnly: boolean; onChanged: () => void }) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [scheduledAt, setScheduledAt] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const canEdit = !readOnly && application.status === "INTERVIEW";
  async function add() {
    setBusy(true); setError("");
    try {
      await hrApplicationService.createRound(application.id, {
        name: name.trim(), interviewerEmail: email.trim(), scheduledAt: scheduledAt ? new Date(scheduledAt).toISOString() : null,
      });
      setName(""); setEmail(""); setScheduledAt(""); onChanged();
    } catch (reason) { setError(getErrorMessage(reason)); }
    finally { setBusy(false); }
  }
  return <section className="rounded-xl border bg-background p-5"><h2 className="text-lg font-semibold">Vòng phỏng vấn</h2>
    {application.rounds.length === 0 && <p className="mt-3 text-sm text-muted-foreground">Chưa có vòng phỏng vấn.</p>}
    <div className="mt-4 space-y-4">{application.rounds.map(round => <article key={round.id} className="rounded-lg border p-4">
      <div className="flex flex-wrap items-center justify-between gap-2"><h3 className="font-medium">Vòng {round.roundNo}: {round.name}</h3><span className="text-sm font-semibold">{round.result}</span></div>
      <p className="mt-2 text-sm text-muted-foreground">{round.interviewerEmail}{round.scheduledAt ? ` · ${formatApplicationDate(round.scheduledAt)}` : ""}</p>
      {round.score !== null && <p className="mt-2 text-sm">Điểm: {round.score}/100</p>}
      {round.note && <p className="mt-2 whitespace-pre-wrap text-sm">{round.note}</p>}
      {canEdit && <RoundEditor applicationId={application.id} round={round} onSaved={onChanged} />}
      {application.roundRevisions.filter(item => item.roundId === round.id).map(item => <p key={item.id} className="mt-3 text-xs text-muted-foreground">
        {formatApplicationDate(item.createdAt)} · {item.oldResult} → {item.newResult}{item.oldScore !== item.newScore ? ` · ${item.oldScore ?? "—"} → ${item.newScore ?? "—"} điểm` : ""}
        {item.oldNote !== item.newNote ? ` · Ghi chú: ${item.oldNote || "—"} → ${item.newNote || "—"}` : ""}
      </p>)}
    </article>)}</div>
    {canEdit && <div className="mt-6 grid gap-3 border-t pt-5 sm:grid-cols-3"><h3 className="font-medium sm:col-span-3">Thêm vòng phỏng vấn</h3>
      <label className="text-sm">Tên vòng<input aria-label="Tên vòng" value={name} onChange={e => setName(e.target.value)} maxLength={200} className="mt-1 w-full rounded-lg border bg-background px-3 py-2" /></label>
      <label className="text-sm">Email người phỏng vấn<input aria-label="Email người phỏng vấn" type="email" value={email} onChange={e => setEmail(e.target.value)} className="mt-1 w-full rounded-lg border bg-background px-3 py-2" /></label>
      <label className="text-sm">Lịch dự kiến<input aria-label="Lịch dự kiến" type="datetime-local" value={scheduledAt} onChange={e => setScheduledAt(e.target.value)} className="mt-1 w-full rounded-lg border bg-background px-3 py-2" /></label>
      {error && <p role="alert" className="text-sm text-red-700 sm:col-span-3">{error}</p>}
      <Button disabled={busy || !name.trim() || !email.trim()} onClick={add} className="sm:w-fit">{busy ? "Đang thêm…" : "Thêm vòng"}</Button>
    </div>}
  </section>;
}
