"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { ChevronDown, ClipboardCheck, Lock } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { TaskBanner } from "@/app/components/ui/task-banner";
import { useMentorRounds } from "@/hooks/use-mentor-rounds";
import type { MentorRoundItem } from "@/interface/hr-application";
import { getErrorMessage } from "@/services/api";
import { DateBlock, RoundEvaluation, RoundInfo, StageBadge } from "./mentor-parts";
import { byScheduleAsc, canEdit, stageOf } from "./mentor-utils";
import "./mentor.css";

const TABS = [
  { key: "review", label: "Cần chấm", empty: "Không còn vòng nào chờ chấm điểm." },
  { key: "upcoming", label: "Sắp diễn ra", empty: "Chưa có buổi phỏng vấn sắp tới." },
  { key: "done", label: "Đã chấm", empty: "Bạn chưa chấm vòng nào." },
  { key: "all", label: "Tất cả", empty: "Chưa có vòng phỏng vấn được mời." },
] as const;
type TabKey = typeof TABS[number]["key"];

function inTab(item: MentorRoundItem, tab: TabKey) {
  const stage = stageOf(item);
  if (tab === "review") return stage === "review";
  if (tab === "upcoming") return stage === "upcoming" || stage === "unscheduled";
  if (tab === "done") return item.round.result !== "PENDING";
  return true;
}

export function MentorReviews() {
  return <Suspense fallback={null}><MentorReviewsContent /></Suspense>;
}

function MentorReviewsContent() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const { data, error, isPending, refetch } = useMentorRounds();
  const items = useMemo(() => data?.items ?? [], [data]);
  const focusId = params.get("round");
  const focused = items.find(item => item.round.id === focusId);
  const requested = params.get("tab");
  // Mở từ liên kết một vòng cụ thể: chọn tab "Tất cả" để chắc chắn thấy vòng đó.
  const tab: TabKey = TABS.some(t => t.key === requested) ? requested as TabKey : focused ? "all" : "review";
  const counts = useMemo(() => Object.fromEntries(TABS.map(t => [t.key, items.filter(item => inTab(item, t.key)).length])) as Record<TabKey, number>, [items]);
  const visible = useMemo(() => {
    const list = items.filter(item => inTab(item, tab));
    return tab === "done" ? list.sort((a, b) => byScheduleAsc(b, a)) : list.sort(byScheduleAsc);
  }, [items, tab]);

  useEffect(() => {
    if (!focused) return;
    document.getElementById(`round-${focused.round.id}`)?.scrollIntoView({ behavior: "smooth", block: "start" });
  }, [focused]);

  function selectTab(key: TabKey) {
    const next = new URLSearchParams(params.toString());
    next.delete("round");
    if (key === "review") next.delete("tab"); else next.set("tab", key);
    router.replace(`${pathname}${next.size ? `?${next}` : ""}`, { scroll: false });
  }

  return <div className="hr-page">
    <TaskBanner title="Đánh giá phỏng vấn" eyebrow="Hội đồng chuyên môn"
      description="Ghi nhận kết quả, điểm và nhận xét cho các vòng phỏng vấn doanh nghiệp mời bạn tham gia." illustration="interviews" compact />

    <div className="mtr-tabs" role="tablist" aria-label="Lọc vòng phỏng vấn">
      {TABS.map(t => <button key={t.key} type="button" role="tab" className="mtr-tab" aria-selected={tab === t.key} onClick={() => selectTab(t.key)}>
        {t.label}<span>{isPending ? "–" : counts[t.key]}</span>
      </button>)}
    </div>

    {error ? <div role="alert" className="hr-surface"><p>{getErrorMessage(error)}</p><Button variant="outline" className="mt-3" onClick={() => refetch()}>Thử lại</Button></div>
      : isPending ? <div role="status" aria-label="Đang tải" className="space-y-3">{[0, 1, 2].map(i => <div key={i} className="h-24 rounded-[14px] bg-muted motion-safe:animate-pulse" />)}</div>
      : visible.length === 0 ? <div className="hr-surface py-12 text-center">
          <ClipboardCheck className="mx-auto mb-4 size-9 text-[var(--brand)]" aria-hidden="true" />
          <h2 className="text-lg font-semibold">{TABS.find(t => t.key === tab)?.empty}</h2>
          <p className="hr-description">Khi doanh nghiệp mời bạn tham gia chấm phỏng vấn, vòng phỏng vấn sẽ xuất hiện tại đây.</p>
        </div>
      : <div className="space-y-4" role="tabpanel">
          {visible.map(item => <ReviewCard key={item.round.id} item={item} focused={item.round.id === focusId} />)}
        </div>}
  </div>;
}

function ReviewCard({ item, focused }: { item: MentorRoundItem; focused: boolean }) {
  const editable = canEdit(item);
  // Mở sẵn khung chấm cho vòng cần chấm hoặc vòng được mở từ liên kết.
  const [open, setOpen] = useState(editable && (focused || stageOf(item) === "review"));
  const panelId = `round-panel-${item.round.id}`;
  return <article id={`round-${item.round.id}`} className="mtr-card" data-focus={focused || undefined} aria-label={`Vòng ${item.round.roundNo} · ${item.round.name}`}>
    <div className="mtr-card-head">
      <DateBlock value={item.round.scheduledAt} />
      <RoundInfo item={item} as="h2" />
      <div className="mtr-card-actions">
        <StageBadge item={item} />
        {editable && <button type="button" className="mtr-toggle" aria-expanded={open} aria-controls={panelId} onClick={() => setOpen(value => !value)}>
          {item.round.result === "PENDING" ? "Chấm điểm" : "Sửa đánh giá"}<ChevronDown aria-hidden="true" /></button>}
      </div>
    </div>
    {!open && item.round.note && <p className="mtr-summary">{item.round.note}</p>}
    {!editable && <p className="mtr-locked"><Lock aria-hidden="true" />Đơn đã chuyển khỏi bước phỏng vấn nên không thể sửa đánh giá.</p>}
    {editable && open && <div id={panelId}><RoundEvaluation item={item} /></div>}
  </article>;
}
