"use client";

import { useCallback, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useRouter, useSearchParams } from "next/navigation";
import { CalendarDays } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/app/components/ui/tabs";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { useAuthStore } from "@/hooks/use-auth-store";
import { getErrorMessage } from "@/services/api";
import { interviewService } from "@/services/interview.service";
import { useJourneyNow } from "../applications/use-journey-now";
import { InterviewInvitationCard } from "./interview-invitation";
import "../applications/candidate-journey.css";
import { TaskBanner } from "@/app/components/ui/task-banner";

export function CandidateInterviews() {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const user = useAuthStore(s => s.user), token = useAuthStore(s => s.token);
  return allowed ? <InterviewList key={`${user?.id}:${token}`} /> : null;
}

function InterviewList() {
  const user = useAuthStore(s => s.user), token = useAuthStore(s => s.token);
  const query = useQuery({ queryKey: ["interviews", "mine", user?.id, token], queryFn: ({ signal }) => interviewService.mineAll(signal), staleTime: 0, refetchOnMount: "always" });
  const now = useJourneyNow();
  const router = useRouter(), params = useSearchParams();
  const raw = params.get("view"), view = raw === "upcoming" || raw === "history" ? raw : "action";
  const [confirmed, setConfirmed] = useState(false);
  const successAction = useRef<HTMLButtonElement>(null), root = useRef<HTMLDivElement>(null);
  const restoreFocus = useCallback(() => {
    requestAnimationFrame(() => {
      const target = root.current?.querySelector<HTMLElement>('[role="tab"][aria-selected="true"]') ?? root.current?.querySelector<HTMLElement>("h1");
      target?.focus();
    });
  }, []);
  const groups = { action: [] as NonNullable<typeof query.data>, upcoming: [] as NonNullable<typeof query.data>, history: [] as NonNullable<typeof query.data> };
  for (const item of query.data ?? []) {
    const schedule = item.schedule;
    const group = schedule?.status === "DONE" ? "history" : schedule?.status === "SCHEDULED" ? (new Date(schedule.endAt).getTime() > now ? "upcoming" : "history") : "action";
    groups[group].push(item);
  }
  groups.upcoming.sort((a, b) => new Date(a.schedule!.startAt).getTime() - new Date(b.schedule!.startAt).getTime());
  groups.history.sort((a, b) => new Date(b.schedule!.startAt).getTime() - new Date(a.schedule!.startAt).getTime());
  const views = [{ value: "action", label: "Cần xử lý" }, { value: "upcoming", label: "Sắp diễn ra" }, { value: "history", label: "Lịch sử" }] as const;
  function navigate(value: string) { router.push(`/candidate/interviews${value === "action" ? "" : `?view=${value}`}`, { scroll: false }); }
  return <div className="journey" ref={root}>
    <TaskBanner title="Phỏng vấn của tôi" eyebrow="Sẵn sàng cho cơ hội mới" description="Sẵn sàng cho cuộc hẹn tiếp theo. Chọn khung giờ, theo dõi lịch hẹn và chuẩn bị phỏng vấn." illustration="interviews" focusableTitle>
      <span>Giờ Việt Nam · UTC+7</span>
    </TaskBanner>
    {confirmed && <div role="status" className="journey-success mb-5"><p>Đã xác nhận khung giờ phỏng vấn.</p><button ref={successAction} className="journey-link" onClick={() => navigate("upcoming")}>Xem lịch sắp diễn ra →</button></div>}
    {query.isPending && <p role="status" aria-busy="true" className="journey-card">Đang tải lời mời…</p>}
    {query.isError && <div role="alert" className="journey-error"><p>{getErrorMessage(query.error)}</p><Button variant="outline" onClick={() => query.refetch()}>Thử lại</Button></div>}
    {query.data && !query.isError && <Tabs className="journey-tabs" value={view} onValueChange={value => navigate(String(value))}>
      <TabsList aria-label="Nhóm lịch phỏng vấn">{views.map(tab => <TabsTrigger key={tab.value} value={tab.value}>{tab.label}<span className="journey-tab-count">{groups[tab.value].length}</span></TabsTrigger>)}</TabsList>
      {views.map(tab => <TabsContent key={tab.value} value={tab.value}><div className="journey-list">{groups[tab.value].length ? groups[tab.value].map(item => <InterviewInvitationCard key={item.roundId} item={item} now={now} refresh={query.refetch} onUnavailable={restoreFocus} onConfirmed={() => { setConfirmed(true); requestAnimationFrame(() => successAction.current?.focus()); }} />) : <section className="journey-card journey-empty"><CalendarDays aria-hidden="true" /><h2>{tab.value === "action" ? "Không có lịch cần xử lý" : tab.value === "upcoming" ? "Chưa có lịch phỏng vấn sắp tới" : "Chưa có lịch sử phỏng vấn"}</h2><p>{tab.value === "action" ? "Lời mời và khung giờ mới sẽ xuất hiện tại đây khi nhà tuyển dụng gửi." : "Các lịch phỏng vấn của bạn sẽ được cập nhật tại đây."}</p></section>}</div></TabsContent>)}
    </Tabs>}
  </div>;
}
