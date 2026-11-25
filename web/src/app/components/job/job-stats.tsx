"use client"

import { cn } from "cn"
import { STATUS_LABEL, STATUS_ORDER } from "@/app/components/job/job-labels"
import type { JobStatus, JobStatusSummary } from "@/interface/job"

interface JobStatsProps {
  summary?: JobStatusSummary
  active: JobStatus | undefined
  onSelect(status: JobStatus | undefined): void
}

/** 6 thẻ đếm JD theo trạng thái; bấm một thẻ để lọc danh sách (bấm "Tất cả" để bỏ lọc). */
export function JobStats({ summary, active, onSelect }: JobStatsProps) {
  const cards: { status: JobStatus | undefined; label: string; count: number | undefined }[] = [
    { status: undefined, label: "Tất cả", count: summary?.total },
    ...STATUS_ORDER.map((s) => ({ status: s, label: STATUS_LABEL[s], count: summary?.byStatus[s] })),
  ]
  return (
    <div className="grid grid-cols-3 gap-2 sm:grid-cols-6">
      {cards.map((card) => {
        const selected = card.status === active
        return (
          <button
            key={card.status ?? "ALL"}
            type="button"
            aria-pressed={selected}
            onClick={() => onSelect(card.status)}
            className={cn(
              "flex min-w-0 flex-col items-start gap-0.5 rounded-lg border bg-background px-3 py-2 text-left transition-colors hover:bg-muted/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
              selected && "border-primary ring-1 ring-primary",
            )}
          >
            <span className="text-xs text-muted-foreground">{card.label}</span>
            <span className="text-xl font-semibold leading-tight">{card.count ?? "–"}</span>
            {card.status === "APPROVED" && summary && summary.expiringSoon > 0 ? (
              <span className="text-[11px] font-medium text-red-600">{summary.expiringSoon} sắp hết hạn</span>
            ) : null}
          </button>
        )
      })}
    </div>
  )
}
