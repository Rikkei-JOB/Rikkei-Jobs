"use client";

import { useLayoutEffect, useRef, useState } from "react";
import { Building2, Video } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import type { InterviewInvitation } from "@/interface/interview";
import { ApiError, getErrorMessage } from "@/services/api";
import { interviewService } from "@/services/interview.service";

const date = (value: string, options: Intl.DateTimeFormatOptions) => new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", ...options }).format(new Date(value));
const time = (value: string) => date(value, { hour: "2-digit", minute: "2-digit" });
const duration = (start: string, end: string) => Math.round((new Date(end).getTime() - new Date(start).getTime()) / 60000);
function safeMeetUrl(value: string | null) {
  try { const url = new URL(value ?? ""); return ["http:", "https:"].includes(url.protocol) ? url.href : null; } catch { return null; }
}

export function InterviewInvitationCard({ item, refresh, now, onConfirmed, onUnavailable }: { item: InterviewInvitation; refresh(): Promise<unknown>; now: number; onConfirmed(): void; onUnavailable(): void }) {
  const [selected, setSelected] = useState(""), [busy, setBusy] = useState(false), [error, setError] = useState("");
  const inFlight = useRef(false);
  const card = useRef<HTMLElement>(null);
  useLayoutEffect(() => {
    const element = card.current;
    return () => { if (element?.contains(document.activeElement)) onUnavailable(); };
  }, [onUnavailable]);
  const schedule = item.schedule;
  const canPick = !schedule || schedule.status === "CANCELLED";
  const slots = canPick ? item.slots.filter(s => s.status === "OPEN" && new Date(s.startAt).getTime() > now) : [];
  const days = Map.groupBy(slots, s => date(s.startAt, { weekday: "long", day: "2-digit", month: "2-digit", year: "numeric" }));
  const upcoming = schedule?.status === "SCHEDULED" && new Date(schedule.endAt).getTime() > now;
  const meet = schedule && upcoming ? safeMeetUrl(schedule.meetUrl) : null;
  const label = schedule?.status === "DONE" ? "Đã hoàn thành" : schedule?.status === "CANCELLED" ? "Đã huỷ" : schedule ? (upcoming ? "Đã xác nhận" : "Đã qua giờ hẹn") : slots.length ? "Chờ chọn giờ" : "Chờ khung giờ";
  async function pick() {
    if (inFlight.current || !slots.some(s => s.id === selected && new Date(s.startAt).getTime() > Date.now())) return;
    inFlight.current = true; setBusy(true); setError("");
    try { await interviewService.pick(item.roundId, selected); onConfirmed(); await refresh(); }
    catch (reason) { setError(getErrorMessage(reason)); if (reason instanceof ApiError && reason.status === 409) { setSelected(""); await refresh(); } }
    finally { setBusy(false); inFlight.current = false; }
  }
  return <article className="journey-card" ref={card}>
    <div className="journey-card-top"><div><p className="journey-company !mt-0"><Building2 aria-hidden="true" />{item.companyName}</p><h2 className="mt-2">{item.roundName}</h2><p className="mt-1">{item.jobTitle}</p></div><span className="journey-badge" data-tone={upcoming ? "info" : canPick ? "warning" : "muted"}>{label}</span></div>
    <p className="journey-small journey-muted mt-4">Người phỏng vấn: {item.interviewerEmail}</p>
    {schedule && <div className="journey-schedule"><div className="journey-date" aria-hidden="true"><strong>{date(schedule.startAt, { day: "2-digit" })}</strong><span>{date(schedule.startAt, { month: "2-digit", year: "numeric" })}</span></div><div><p className="font-semibold">{date(schedule.startAt, { weekday: "long", day: "2-digit", month: "2-digit", year: "numeric" })}</p><p>{time(schedule.startAt)} – {time(schedule.endAt)} · {duration(schedule.startAt, schedule.endAt)} phút</p>{upcoming && (meet ? <a className="journey-link" href={meet} target="_blank" rel="noopener noreferrer"><Video aria-hidden="true" />Vào phòng phỏng vấn</a> : <p className="journey-small journey-muted">Đường dẫn phòng phỏng vấn chưa được cập nhật.</p>)}{schedule.status === "CANCELLED" && <p className="journey-small journey-muted">Lịch cũ đã huỷ. {slots.length ? "Vui lòng chọn một khung giờ mới bên dưới." : "Vui lòng chờ HR đề xuất khung giờ mới."}</p>}</div></div>}
    {slots.length > 0 && <fieldset disabled={busy} className="mt-5"><legend className="font-semibold mb-4">Chọn khung giờ phù hợp</legend>{[...days].map(([day, items]) => <div className="mb-4" key={day}><p className="journey-small journey-muted mb-2">{day}</p><div className="journey-slot-grid">{items.map(s => <label key={s.id} className="journey-slot"><input type="radio" name={`slot-${item.roundId}`} value={s.id} checked={selected === s.id} onChange={() => setSelected(s.id)} /><span><strong>{time(s.startAt)} – {time(s.endAt)}</strong><span className="journey-small journey-muted">{duration(s.startAt, s.endAt)} phút · Giờ Việt Nam</span></span></label>)}</div></div>)}<Button disabled={busy || !slots.some(s => s.id === selected)} onClick={pick}>{busy ? "Đang xác nhận…" : "Xác nhận khung giờ"}</Button></fieldset>}
    {canPick && !slots.length && !schedule && <p className="journey-info mt-4">Chưa có khung giờ khả dụng. Vui lòng chờ HR đề xuất.</p>}
    {error && <p role="alert" className="journey-error mt-4">{error}</p>}
  </article>;
}
