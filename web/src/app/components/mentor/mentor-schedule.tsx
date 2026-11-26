"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { CalendarX2, ChevronLeft, ChevronRight, CircleDashed } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { TaskBanner } from "@/app/components/ui/task-banner";
import { useMentorRounds } from "@/hooks/use-mentor-rounds";
import { useNow } from "@/hooks/use-now";
import type { MentorRoundItem } from "@/interface/hr-application";
import { getErrorMessage } from "@/services/api";
import { RoundInfo, StageBadge } from "./mentor-parts";
import { byScheduleAsc, dateParts, dayKey, formatDayLabel, formatTime, relativeDay } from "./mentor-utils";
import "./mentor.css";

const DAY = 86_400_000;

/** Thứ Hai đầu tuần (theo ngày lịch Việt Nam) chứa `date`, cộng thêm `offset` tuần. */
function weekStart(offset: number) {
  const today = new Date(`${dayKey(new Date())}T00:00:00Z`);
  const weekday = (today.getUTCDay() + 6) % 7; // 0 = Thứ Hai
  return new Date(today.getTime() - weekday * DAY + offset * 7 * DAY);
}

export function MentorSchedule() {
  const { data, error, isPending, refetch } = useMentorRounds();
  const now = useNow();
  const [offset, setOffset] = useState(0);
  const [selectedDay, setSelectedDay] = useState<string | null>(null);
  const items = useMemo(() => data?.items ?? [], [data]);
  const todayKey = dayKey(new Date());

  const days = useMemo(() => Array.from({ length: 7 }, (_, i) => {
    const date = new Date(weekStart(offset).getTime() + i * DAY);
    const key = date.toISOString().slice(0, 10);
    return { key, date, rounds: items.filter(item => item.round.scheduledAt && dayKey(item.round.scheduledAt) === key).sort(byScheduleAsc) };
  }), [items, offset]);
  const unscheduled = items.filter(item => !item.round.scheduledAt);
  const shown = selectedDay ? days.filter(day => day.key === selectedDay) : days.filter(day => day.rounds.length);
  const weekTotal = days.reduce((sum, day) => sum + day.rounds.length, 0);
  const range = `${formatDayLabel(days[0].date)} – ${formatDayLabel(days[6].date)}`;

  function move(step: number) { setOffset(value => value + step); setSelectedDay(null); }

  return <div className="hr-page">
    <TaskBanner title="Lịch phỏng vấn" eyebrow="Không gian Mentor"
      description="Theo dõi các buổi phỏng vấn doanh nghiệp mời bạn tham gia theo từng tuần." illustration="interviews" compact />

    {error ? <div role="alert" className="hr-surface"><p>{getErrorMessage(error)}</p><Button variant="outline" className="mt-3" onClick={() => refetch()}>Thử lại</Button></div> : <>
      <section className="mtr-panel" aria-label="Lịch theo tuần">
        <div className="mtr-panel-head">
          <h2>{offset === 0 ? "Tuần này" : offset === 1 ? "Tuần sau" : offset === -1 ? "Tuần trước" : "Tuần"} <span className="text-sm font-medium text-[var(--text-muted)]">{range}</span></h2>
          {offset !== 0 && <button type="button" className="text-sm font-semibold text-[var(--brand)] hover:underline" onClick={() => move(-offset)}>Về tuần này</button>}
        </div>
        <div className="p-4">
          <div className="mtr-week">
            <button type="button" className="mtr-week-nav" aria-label="Tuần trước" onClick={() => move(-1)}><ChevronLeft aria-hidden="true" /></button>
            <div className="mtr-week-days" role="group" aria-label="Chọn ngày">
              {days.map(day => {
                const { day: dd, weekday } = dateParts(day.date);
                return <button key={day.key} type="button" className="mtr-day" aria-pressed={selectedDay === day.key}
                  data-has={day.rounds.length ? "" : undefined} data-today={day.key === todayKey || undefined}
                  aria-label={`${formatDayLabel(day.date)}: ${day.rounds.length} buổi phỏng vấn`}
                  onClick={() => setSelectedDay(value => (value === day.key ? null : day.key))}>
                  <small>{weekday}</small><strong>{dd}</strong><i />
                </button>;
              })}
            </div>
            <button type="button" className="mtr-week-nav" aria-label="Tuần sau" onClick={() => move(1)}><ChevronRight aria-hidden="true" /></button>
          </div>

          {isPending ? <div className="mt-4 h-40 rounded-xl bg-muted motion-safe:animate-pulse" role="status" aria-label="Đang tải" />
            : shown.every(day => !day.rounds.length) ? <div className="mtr-panel-empty mt-2">
                <CalendarX2 aria-hidden="true" />
                {selectedDay ? "Ngày này không có buổi phỏng vấn." : weekTotal === 0 ? "Tuần này không có buổi phỏng vấn." : null}
                <button type="button" className="text-sm font-semibold text-[var(--brand)] hover:underline" onClick={() => move(1)}>Xem tuần sau</button>
              </div>
            : shown.map(day => <div key={day.key} className="mtr-agenda-day">
                <h3>{formatDayLabel(day.date)} <span>{relativeDay(`${day.key}T05:00:00Z`)} · {day.rounds.length} buổi</span></h3>
                {day.rounds.map(item => <AgendaItem key={item.round.id} item={item} now={now} />)}
              </div>)}
        </div>
      </section>

      {unscheduled.length > 0 && <section className="mtr-panel" aria-label="Chưa hẹn lịch">
        <div className="mtr-panel-head"><h2><CircleDashed aria-hidden="true" />Chưa hẹn lịch <span className="text-sm font-medium text-[var(--text-muted)]">{unscheduled.length} vòng</span></h2></div>
        <div className="mtr-panel-body">{unscheduled.map(item => <AgendaItem key={item.round.id} item={item} now={now} />)}</div>
      </section>}
    </>}
  </div>;
}

function AgendaItem({ item, now }: { item: MentorRoundItem; now: number }) {
  const at = item.round.scheduledAt;
  return <Link href={`/mentor/reviews?round=${item.round.id}`} className="mtr-agenda-item" data-past={at && Date.parse(at) < now ? "" : undefined}>
    <span className="mtr-agenda-time">{at ? formatTime(at) : "--:--"}</span>
    <RoundInfo item={item} />
    <StageBadge item={item} />
  </Link>;
}
