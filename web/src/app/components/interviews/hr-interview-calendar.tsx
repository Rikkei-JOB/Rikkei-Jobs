"use client";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/app/components/ui/button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { getErrorMessage } from "@/services/api";
import { interviewService } from "@/services/interview.service";
import { currentMonday, dayLabel, interviewTime, interviewClock, shiftDay } from "./interview-time";
export function HrInterviewCalendar() {
  const allowed = useAuthGuard({ roles: ["HR"] });
  const [week, setWeek] = useState(currentMonday), [email, setEmail] = useState(""), [filter, setFilter] = useState(""), [status, setStatus] = useState("");
  const from = `${week}T00:00:00+07:00`, to = `${shiftDay(week, 7)}T00:00:00+07:00`;
  const query = useQuery({ queryKey: ["interviews", "company", week, filter, status], queryFn: ({ signal }) => interviewService.companyAll({ from, to, interviewerEmail: filter || undefined, status: status || undefined }, signal), enabled: allowed });
  if (!allowed) return null;
  return <main className="space-y-5 px-4 py-8 sm:px-6"><header><h1 className="text-2xl font-semibold">Lịch phỏng vấn</h1><p className="mt-2 text-sm text-muted-foreground">Toàn công ty · Asia/Ho_Chi_Minh (UTC+07:00)</p></header>
    <div className="flex flex-wrap items-center gap-2"><Button variant="outline" onClick={() => setWeek(shiftDay(week, -7))}>Tuần trước</Button><Button variant="outline" onClick={() => setWeek(currentMonday())}>Hôm nay</Button><Button variant="outline" onClick={() => setWeek(shiftDay(week, 7))}>Tuần sau</Button><strong data-testid="calendar-range" className="ml-2 text-sm">{dayLabel(week)} – {dayLabel(shiftDay(week, 6))}</strong></div>
    <form className="flex flex-wrap items-end gap-3" onSubmit={e => { e.preventDefault(); setFilter(email.trim()); }}><label className="text-sm">Người phỏng vấn<input aria-label="Lọc email người phỏng vấn" type="email" placeholder="Tất cả người phỏng vấn" value={email} onChange={e => setEmail(e.target.value)} className="mt-1 block w-64 max-w-full rounded-lg border bg-background p-2" /></label><label className="text-sm">Trạng thái<select aria-label="Lọc trạng thái lịch" value={status} onChange={e => setStatus(e.target.value)} className="mt-1 block rounded-lg border bg-background p-2"><option value="">Đã xác nhận và hoàn thành</option><option value="SCHEDULED">Đã xác nhận</option><option value="DONE">Đã hoàn thành</option><option value="CANCELLED">Đã huỷ</option></select></label><Button type="submit" variant="outline">Lọc</Button></form>
    {query.isPending && <p role="status">Đang tải lịch…</p>}{query.isError && <div role="alert"><p>{getErrorMessage(query.error)}</p><Button variant="outline" onClick={() => query.refetch()}>Thử lại</Button></div>}{query.data?.length === 0 && <p className="rounded-xl border p-6 text-muted-foreground">Tuần này chưa có lịch phù hợp.</p>}
    {query.data && <div className="overflow-x-auto rounded-xl border bg-background"><div className="grid min-w-[980px] grid-cols-7">{Array.from({ length: 7 }, (_, i) => shiftDay(week, i)).map(day => {
      const start = Date.parse(`${day}T00:00:00+07:00`), end = start + 86400000;
      const items = query.data.filter(item => Date.parse(item.schedule.startAt) < end && Date.parse(item.schedule.endAt) > start);
      return <section key={day} aria-label={dayLabel(day)} className="min-h-52 border-r p-3 last:border-r-0"><h2 className="border-b pb-3 text-sm font-semibold">{dayLabel(day)}</h2><div className="mt-3 space-y-3">{items.map(item => <article key={item.schedule.id} className={`break-words rounded-lg border-l-4 p-3 text-xs ${item.schedule.status === "CANCELLED" ? "border-red-400 bg-red-50 text-red-950" : "border-primary/60 bg-primary/5"}`}><p className="font-semibold">{item.candidateName}</p><p className="mt-1" title={`${interviewTime(item.schedule.startAt)} - ${interviewTime(item.schedule.endAt)}`}>{interviewClock(item.schedule.startAt)} – {interviewClock(item.schedule.endAt)}</p><p className="mt-2">{item.jobTitle} · {item.roundName}</p><p className="mt-1 text-muted-foreground">{item.interviewerEmail}</p><p className="mt-2">{({ SCHEDULED: "Đã xác nhận", DONE: "Đã hoàn thành", CANCELLED: "Đã huỷ" })[item.schedule.status]}</p></article>)}</div></section>;
    })}</div></div>}
  </main>;
}
