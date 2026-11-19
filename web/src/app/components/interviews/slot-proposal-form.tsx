"use client";
import { useRef, useState } from "react";
import { Button } from "@/app/components/ui/button";
import type { InterviewRound } from "@/interface/hr-application";
import { getErrorMessage } from "@/services/api";
import { interviewService } from "@/services/interview.service";
import { interviewTime, localInterviewInstant } from "./interview-time";
export function SlotProposalForm({ round, onSaved }: { round: InterviewRound; onSaved: () => void }) {
  const [rows, setRows] = useState([{ start: "", end: "" }]);
  const [acknowledged, setAcknowledged] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState(""), [success, setSuccess] = useState("");
  const inFlight = useRef(false);
  const previous = Boolean(round.slots?.length || round.schedule);
  function change(index: number, field: "start" | "end", value: string) { setRows(current => current.map((r, i) => i === index ? { ...r, [field]: value } : r)); setSuccess(""); }
  async function submit() {
    if (inFlight.current) return; setError(""); setSuccess("");
    try {
      const slots = rows.map(r => ({ startAt: localInterviewInstant(r.start), endAt: localInterviewInstant(r.end) }));
      if (slots.some(s => Date.parse(s.startAt) <= Date.now() || Date.parse(s.endAt) - Date.parse(s.startAt) < 1800000)) { setError("Mỗi khung phải ở tương lai và dài ít nhất 30 phút."); return; }
      const sorted = [...slots].sort((a, b) => Date.parse(a.startAt) - Date.parse(b.startAt));
      if (sorted.some((s, i) => i > 0 && Date.parse(s.startAt) < Date.parse(sorted[i - 1].endAt))) { setError("Các khung giờ không được chồng lấn."); return; }
      if (previous && !acknowledged) { setError("Vui lòng xác nhận huỷ lịch và các khung cũ."); return; }
      inFlight.current = true; setBusy(true);
      await (previous ? interviewService.reschedule(round.id, slots) : interviewService.propose(round.id, slots));
      setSuccess("Đã gửi đề xuất giờ cho ứng viên."); setAcknowledged(false); onSaved();
    } catch (reason) { setError(reason instanceof RangeError ? "Vui lòng nhập đủ thời gian bắt đầu và kết thúc." : getErrorMessage(reason)); }
    finally { inFlight.current = false; setBusy(false); }
  }
  return <div className="mt-4 space-y-3 rounded-lg bg-muted/40 p-4">
    <h4 className="font-medium">Đề xuất giờ phỏng vấn</h4><p className="text-sm text-muted-foreground">1–5 khung, mỗi khung ít nhất 30 phút · Asia/Ho_Chi_Minh (UTC+07:00)</p>
    {round.schedule && <p className="text-sm">Lịch: {interviewTime(round.schedule.startAt)} · {({ SCHEDULED: "Đã xác nhận", DONE: "Đã hoàn thành", CANCELLED: "Đã huỷ" })[round.schedule.status]}</p>}
    {round.slots?.filter(s => s.status === "OPEN").map(s => <p key={s.id} className="text-sm">Đang chờ chọn: {interviewTime(s.startAt)} – {interviewTime(s.endAt)}</p>)}
    {rows.map((r, i) => <div key={i} className="grid gap-3 sm:grid-cols-[1fr_1fr_auto]">
      <label className="text-sm">Bắt đầu khung {i + 1}<input aria-label={`Bắt đầu khung ${i + 1}`} type="datetime-local" value={r.start} onChange={e => change(i, "start", e.target.value)} disabled={busy} className="mt-1 w-full rounded-lg border bg-background p-2" /></label>
      <label className="text-sm">Kết thúc khung {i + 1}<input aria-label={`Kết thúc khung ${i + 1}`} type="datetime-local" value={r.end} onChange={e => change(i, "end", e.target.value)} disabled={busy} className="mt-1 w-full rounded-lg border bg-background p-2" /></label>
      <Button variant="outline" className="self-end" disabled={rows.length === 1 || busy} onClick={() => setRows(rows.filter((_, index) => index !== i))} aria-label={`Xoá khung ${i + 1}`}>Xoá</Button>
    </div>)}
    <Button variant="outline" disabled={busy || rows.length >= 5} onClick={() => setRows([...rows, { start: "", end: "" }])}>Thêm khung giờ</Button>
    {previous && <label className="flex items-start gap-2 text-sm"><input type="checkbox" checked={acknowledged} disabled={busy} onChange={e => setAcknowledged(e.target.checked)} />Tôi hiểu khi gửi, lịch và các khung cũ sẽ bị huỷ ngay; ứng viên phải chọn lại.</label>}
    {error && <p role="alert" className="text-sm text-red-700">{error}</p>}{success && <p role="status" className="text-sm text-green-700">{success}</p>}
    <div><Button disabled={busy} onClick={submit}>{busy ? "Đang gửi…" : previous ? "Đổi lịch" : "Gửi đề xuất"}</Button></div>
  </div>;
}
