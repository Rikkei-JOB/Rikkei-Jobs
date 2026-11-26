"use client"

import { Button } from "@/app/components/ui/button"
import { formatDateTime } from "@/app/components/job/job-labels"
import type { StaffJobItem } from "@/interface/staff-job"

interface ReviewQueueProps {
  items: StaffJobItem[]
  selectedId: string | undefined
  onSelect: (id: string) => void
  page: number
  lastPage: number
  total: number
  onPage: (page: number) => void
  /** Đang tải lại (giữ dữ liệu cũ, làm mờ). */
  fetching?: boolean
  emptyText: string
  /** Chữ trước thời điểm: "gửi" ở hàng chờ duyệt, "cập nhật" ở các tab khác (submittedAt = lần cập nhật cuối). */
  timeLabel: string
}

/** Hàng chờ JD bên trái màn "Duyệt JD": tiêu đề, công ty, thời điểm gửi (giờ VN) và phân trang. */
export function ReviewQueue({ items, selectedId, onSelect, page, lastPage, total, onPage, fetching, emptyText, timeLabel }: ReviewQueueProps) {
  return (
    <div className={fetching ? "opacity-60" : undefined}>
      {items.length === 0 ? (
        <p className="rounded-xl border border-dashed bg-card py-10 text-center text-xs text-muted-foreground">{emptyText}</p>
      ) : (
        <ul className="space-y-2" aria-label="Danh sách JD">
          {items.map((item) => {
            const active = item.id === selectedId
            return (
              <li key={item.id}>
                <button
                  type="button"
                  onClick={() => onSelect(item.id)}
                  aria-current={active ? "true" : undefined}
                  className={`block w-full rounded-lg border px-3 py-2.5 text-left transition-colors outline-none focus-visible:ring-2 focus-visible:ring-ring/30 ${
                    active ? "border-primary bg-primary/5" : "border-border bg-card hover:bg-muted/40"
                  }`}
                >
                  <span className="block truncate text-sm font-semibold text-[#18233a]" title={item.title}>
                    {item.title}
                  </span>
                  <span className="mt-0.5 block truncate text-xs text-muted-foreground">
                    {item.company.name ?? "Công ty không xác định"} · {timeLabel} {formatDateTime(item.submittedAt)}
                  </span>
                </button>
              </li>
            )
          })}
        </ul>
      )}
      {total > 0 ? (
        <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
          <span>
            {total} JD · Trang {page}/{lastPage}
          </span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => onPage(page - 1)}>
              Trước
            </Button>
            <Button variant="outline" size="sm" disabled={page >= lastPage} onClick={() => onPage(page + 1)}>
              Sau
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
