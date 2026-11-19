"use client";
import { useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/app/components/ui/button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import type { InterviewInvitation } from "@/interface/interview";
import { ApiError, getErrorMessage } from "@/services/api";
import { interviewService } from "@/services/interview.service";
import { interviewTime } from "./interview-time";
function Invitation({ item, refresh }: { item: InterviewInvitation; refresh: () => Promise<unknown> }) {
  const [selected, setSelected] = useState(""), [busy, setBusy] = useState(false), [error, setError] = useState(""); const inFlight = useRef(false);
  async function pick() {
    if (inFlight.current) return; inFlight.current = true; setBusy(true); setError("");
    try { await interviewService.pick(item.roundId, selected); await refresh(); }
    catch (reason) { setError(getErrorMessage(reason)); if (reason instanceof ApiError && reason.status === 409) { setSelected(""); await refresh(); } }
    finally { setBusy(false); inFlight.current = false; }
  }
  const schedule = item.schedule;
  return <article className="rounded-xl border bg-background p-5 shadow-sm">
    <p className="text-sm text-muted-foreground">{item.companyName} · {item.jobTitle}</p><h2 className="mt-1 text-lg font-semibold">{item.roundName}</h2><p className="mt-2 text-sm">Người phỏng vấn: {item.interviewerEmail}</p>
    {schedule && <div className="mt-4 rounded-lg bg-muted/50 p-3"><p>{interviewTime(schedule.startAt)} – {interviewTime(schedule.endAt)}</p><p className="mt-1 text-sm">{({ SCHEDULED: "Đã xác nhận", DONE: "Đã hoàn thành", CANCELLED: "Đã huỷ — chờ chọn lại giờ" })[schedule.status]}</p>{schedule.status === "SCHEDULED" && schedule.meetUrl && <a className="mt-3 inline-flex rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground" href={schedule.meetUrl} target="_blank" rel="noopener noreferrer">Vào phòng họp</a>}</div>}
    {item.slots.length > 0 && <fieldset disabled={busy} className="mt-4 space-y-2"><legend className="mb-2 font-medium">Chọn khung giờ phù hợp</legend>{item.slots.map(s => <label key={s.id} className="flex cursor-pointer items-center gap-3 rounded-lg border p-3 text-sm"><input type="radio" name={`slot-${item.roundId}`} value={s.id} checked={selected === s.id} onChange={() => setSelected(s.id)} />{interviewTime(s.startAt)} – {interviewTime(s.endAt)}</label>)}<Button disabled={busy || !item.slots.some(s => s.id === selected)} onClick={pick}>{busy ? "Đang xác nhận…" : "Xác nhận khung giờ"}</Button></fieldset>}
    {!schedule && item.slots.length === 0 && <p className="mt-3 text-sm text-muted-foreground">Chưa có khung giờ khả dụng. Vui lòng chờ HR đề xuất.</p>}{error && <p role="alert" className="mt-3 text-sm text-red-700">{error}</p>}
  </article>;
}
export function CandidateInterviews() {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const query = useQuery({ queryKey: ["interviews", "mine"], queryFn: ({ signal }) => interviewService.mineAll(signal), enabled: allowed });
  if (!allowed) return null;
  return <main className="mx-auto max-w-4xl space-y-5 px-4 py-8 sm:px-6"><header><h1 className="text-2xl font-semibold">Phỏng vấn của tôi</h1><p className="mt-2 text-sm text-muted-foreground">Chọn giờ, xem lịch và vào phòng họp · Asia/Ho_Chi_Minh (UTC+07:00)</p></header>
    {query.isPending && <p role="status">Đang tải lời mời…</p>}{query.isError && <div role="alert"><p>{getErrorMessage(query.error)}</p><Button variant="outline" onClick={() => query.refetch()}>Thử lại</Button></div>}
    {query.data?.length === 0 && <p className="rounded-xl border p-6 text-muted-foreground">Bạn chưa có lời mời phỏng vấn.</p>}{query.data?.map(item => <Invitation key={item.roundId} item={item} refresh={query.refetch} />)}
  </main>;
}
