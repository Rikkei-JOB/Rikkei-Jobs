"use client";

import { useMemo } from "react";
import Link from "next/link";
import { ArrowRight, CalendarCheck2, CalendarClock, CalendarDays, ClipboardCheck, ClipboardList, History, Inbox } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { TaskBanner } from "@/app/components/ui/task-banner";
import { useAuthStore } from "@/hooks/use-auth-store";
import { useMentorRounds } from "@/hooks/use-mentor-rounds";
import { useNow } from "@/hooks/use-now";
import type { MentorRoundItem } from "@/interface/hr-application";
import { getErrorMessage } from "@/services/api";
import { RoundRow } from "./mentor-parts";
import { byScheduleAsc, dayKey, stageOf } from "./mentor-utils";
import "./mentor.css";

function Panel({ title, icon: Icon, href, linkLabel, items, empty }: {
  title: string; icon: typeof CalendarDays; href: string; linkLabel: string; items: MentorRoundItem[]; empty: string;
}) {
  return <section className="mtr-panel" aria-label={title}>
    <div className="mtr-panel-head"><h2><Icon aria-hidden="true" />{title}</h2><Link href={href}>{linkLabel}<ArrowRight aria-hidden="true" /></Link></div>
    <div className="mtr-panel-body">
      {items.length ? items.map(item => <RoundRow key={item.round.id} item={item} />)
        : <div className="mtr-panel-empty"><Inbox aria-hidden="true" />{empty}</div>}
    </div>
  </section>;
}

export function MentorOverview() {
  const user = useAuthStore(state => state.user);
  const { data, error, isPending, refetch } = useMentorRounds();
  const now = useNow();

  const groups = useMemo(() => {
    const items = data?.items ?? [];
    const today = dayKey(new Date(now));
    const review = items.filter(item => stageOf(item, now) === "review").sort(byScheduleAsc);
    const upcoming = items.filter(item => stageOf(item, now) === "upcoming").sort(byScheduleAsc);
    const unscheduled = items.filter(item => stageOf(item, now) === "unscheduled");
    const todayItems = items.filter(item => item.round.scheduledAt && dayKey(item.round.scheduledAt) === today);
    const done = items.filter(item => item.round.result !== "PENDING")
      .sort((a, b) => (b.round.scheduledAt ?? b.round.createdAt).localeCompare(a.round.scheduledAt ?? a.round.createdAt));
    return { review, upcoming, unscheduled, todayItems, done };
  }, [data, now]);

  const firstName = (user?.fullName?.trim().split(/\s+/).at(-1)) ?? "Mentor";
  const summary = isPending ? "Đang tải lịch phỏng vấn của bạn…"
    : `Bạn có ${groups.review.length} vòng cần chấm điểm và ${groups.upcoming.length} buổi phỏng vấn sắp tới.`;

  const stats = [
    { label: "Cần chấm điểm", value: groups.review.length, icon: ClipboardCheck, tone: "review", href: "/mentor/reviews?tab=review" },
    { label: "Phỏng vấn hôm nay", value: groups.todayItems.length, icon: CalendarClock, tone: "today", href: "/mentor/schedule" },
    { label: "Sắp diễn ra", value: groups.upcoming.length + groups.unscheduled.length, icon: CalendarDays, tone: "upcoming", href: "/mentor/reviews?tab=upcoming" },
    { label: "Đã đánh giá", value: groups.done.length, icon: CalendarCheck2, tone: "done", href: "/mentor/reviews?tab=done" },
  ];

  return <div className="hr-page">
    <TaskBanner title={`Xin chào, ${firstName}`} eyebrow="Không gian Mentor" description={summary} illustration="interviews" compact>
      <Link href="/mentor/reviews?tab=review"><ClipboardList className="size-4" aria-hidden="true" />Chấm điểm ngay</Link>
      <Link href="/mentor/schedule"><CalendarDays className="size-4" aria-hidden="true" />Xem lịch tuần</Link>
    </TaskBanner>

    {error ? <div role="alert" className="hr-surface"><p>{getErrorMessage(error)}</p><Button variant="outline" className="mt-3" onClick={() => refetch()}>Thử lại</Button></div> : <>
      <div className="mtr-stats" aria-busy={isPending}>
        {stats.map(stat => <Link key={stat.label} href={stat.href} className={`mtr-stat mtr-tone-${stat.tone}`}>
          <span className="mtr-stat-icon"><stat.icon aria-hidden="true" /></span>
          <strong>{isPending ? "–" : stat.value}</strong><span>{stat.label}</span>
        </Link>)}
      </div>

      {isPending ? <div className="mtr-columns">{[0, 1].map(i => <div key={i} className="h-64 rounded-[14px] bg-muted motion-safe:animate-pulse" />)}</div> : <>
        <div className="mtr-columns">
          <Panel title="Lịch sắp tới" icon={CalendarDays} href="/mentor/schedule" linkLabel="Xem lịch" items={groups.upcoming.slice(0, 5)}
            empty="Chưa có buổi phỏng vấn nào sắp tới." />
          <Panel title="Cần chấm điểm" icon={ClipboardCheck} href="/mentor/reviews?tab=review" linkLabel="Tất cả" items={groups.review.slice(0, 5)}
            empty="Bạn đã chấm xong mọi vòng đã diễn ra." />
        </div>
        <Panel title="Đánh giá gần đây" icon={History} href="/mentor/reviews?tab=done" linkLabel="Xem tất cả" items={groups.done.slice(0, 3)}
          empty="Các vòng bạn đã chấm sẽ hiện tại đây." />
      </>}
    </>}
  </div>;
}
