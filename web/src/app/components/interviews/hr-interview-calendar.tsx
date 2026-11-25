"use client";
import { useEffect, useState } from "react";
import { CalendarDays, List } from "lucide-react";
import { useAuthStore } from "@/hooks/use-auth-store";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/app/components/ui/button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { getErrorMessage } from "@/services/api";
import { interviewService } from "@/services/interview.service";
import { currentMonday, dayLabel, interviewTime, interviewClock, shiftDay } from "./interview-time";
export function HrInterviewCalendar() {
  const allowed = useAuthGuard({ roles: ["HR"] });
  const [week, setWeek] = useState(currentMonday), [email, setEmail] = useState(""), [filter, setFilter] = useState(""), [status, setStatus] = useState("");
  const [view, setView] = useState("week");
  const viewerId = useAuthStore(state => state.user?.id), token = useAuthStore(state => state.token);
  const [today, setToday] = useState(() => new Date(Date.now() + 7 * 3600000).toISOString().slice(0, 10));
  useEffect(() => {
    const timer = setInterval(() => setToday(new Date(Date.now() + 7 * 3600000).toISOString().slice(0, 10)), 60_000);
    return () => clearInterval(timer);
  }, []);
  const from = `${week}T00:00:00+07:00`, to = `${shiftDay(week, 7)}T00:00:00+07:00`;
  const query = useQuery({ queryKey: ["interviews", "company", viewerId, token, week, filter, status], queryFn: ({ signal }) => interviewService.companyAll({ from, to, interviewerEmail: filter || undefined, status: status || undefined }, signal), enabled: allowed });
  if (!allowed) return null;
  return <div className="hr-page"><header className="hr-page-heading"><div><p className="hr-eyebrow">KẾ HOẠCH TUYỂN DỤNG</p><h1>Lịch phỏng vấn</h1><p className="hr-description">Theo dõi lịch toàn công ty · Giờ Việt Nam (UTC+07:00)</p></div><div className="flex gap-2" role="group" aria-label="Cách xem lịch"><Button variant={view === "week" ? "default" : "outline"} aria-pressed={view === "week"} onClick={() => setView("week")}><CalendarDays aria-hidden="true" />Tuần</Button><Button variant={view === "list" ? "default" : "outline"} aria-pressed={view === "list"} onClick={() => setView("list")}><List aria-hidden="true" />Danh sách</Button></div></header>
    <div className="flex flex-wrap items-center gap-2"><Button variant="outline" onClick={() => setWeek(shiftDay(week, -7))}>Tuần trước</Button><Button variant="outline" onClick={() => setWeek(currentMonday())}>Hôm nay</Button><Button variant="outline" onClick={() => setWeek(shiftDay(week, 7))}>Tuần sau</Button><strong data-testid="calendar-range" className="ml-2 text-sm">{dayLabel(week)} – {dayLabel(shiftDay(week, 6))}</strong></div>
    <form className="hr-surface hr-filter-bar" onSubmit={e => { e.preventDefault(); setFilter(email.trim()); }}><label className="text-sm">Người phỏng vấn<input aria-label="Lọc email người phỏng vấn" type="email" placeholder="Tất cả người phỏng vấn" value={email} onChange={e => setEmail(e.target.value)} className="mt-1 block w-64 max-w-full rounded-lg border bg-background p-2" /></label><label className="text-sm">Trạng thái<select aria-label="Lọc trạng thái lịch" value={status} onChange={e => setStatus(e.target.value)} className="mt-1 block rounded-lg border bg-background p-2"><option value="">Đã xác nhận và hoàn thành</option><option value="SCHEDULED">Đã xác nhận</option><option value="DONE">Đã hoàn thành</option><option value="CANCELLED">Đã huỷ</option></select></label><Button type="submit" variant="outline">Lọc</Button></form>
    {query.isPending && <p role="status">Đang tải lịch…</p>}{query.isError && <div role="alert"><p>{getErrorMessage(query.error)}</p><Button variant="outline" onClick={() => query.refetch()}>Thử lại</Button></div>}{!query.isError && query.data?.length === 0 && <p className="rounded-xl border p-6 text-muted-foreground">Tuần này chưa có lịch phù hợp.</p>}
    {!query.isError && query.data && query.data.length > 0 && <div className="overflow-x-auto rounded-xl border bg-background" role="region" aria-label="Lịch trong tuần" tabIndex={0}><div className="hr-calendar-grid" data-view={view}>{Array.from({ length: 7 }, (_, i) => shiftDay(week, i)).map(day => {
      const start = Date.parse(`${day}T00:00:00+07:00`), end = start + 86400000;
      const items = query.data.filter(item => Date.parse(item.schedule.startAt) < end && Date.parse(item.schedule.endAt) > start);
      return <section key={day} aria-label={dayLabel(day)} className={`min-h-52 border-r p-3 last:border-r-0 ${day === today ? "bg-primary/5" : ""}`}><h2 className="border-b pb-3 text-sm font-semibold">{dayLabel(day)}{day === today && <span className="ml-2 text-primary">Hôm nay</span>}</h2><div className="hr-calendar-events mt-3">{items.length === 0 && <p className="py-3 text-xs text-muted-foreground">Chưa có lịch</p>}{items.map(item => <article key={item.schedule.id} className={`break-words rounded-lg border-l-4 p-3 text-xs ${item.schedule.status === "CANCELLED" ? "border-red-400 bg-red-50 text-red-950" : "border-primary/60 bg-primary/5"}`}><p className="font-semibold">{item.candidateName}</p><p className="mt-1" title={`${interviewTime(item.schedule.startAt)} - ${interviewTime(item.schedule.endAt)}`}>{interviewClock(item.schedule.startAt)} – {interviewClock(item.schedule.endAt)}</p><p className="mt-2">{item.jobTitle} · {item.roundName}</p><p className="mt-1 text-muted-foreground">{item.interviewerEmail}</p><p className="mt-2">{({ SCHEDULED: "Đã xác nhận", DONE: "Đã hoàn thành", CANCELLED: "Đã huỷ" })[item.schedule.status]}</p></article>)}</div></section>;
    })}</div></div>}
  </div>;
}
