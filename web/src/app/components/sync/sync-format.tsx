import { cn } from "cn";
import type { ExternalSource, SyncCounts, SyncStatus, SyncTrigger } from "@/interface/sync";

const DATE_TIME = new Intl.DateTimeFormat("vi-VN", {
  timeZone: "Asia/Ho_Chi_Minh",
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
});

const DATE = new Intl.DateTimeFormat("vi-VN", {
  timeZone: "Asia/Ho_Chi_Minh",
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
});

export function formatDateTime(value: string | null): string {
  return value ? DATE_TIME.format(new Date(value)) : "—";
}

export function formatDate(value: string | null): string {
  return value ? DATE.format(new Date(value)) : "—";
}

export const TRIGGER_LABEL: Record<SyncTrigger, string> = {
  NIGHTLY: "Job đêm",
  ON_OPEN: "Mở hồ sơ",
  MANUAL: "Đồng bộ lại",
};

export const SOURCE_LABEL: Record<ExternalSource, string> = {
  LMS: "LMS",
  EXAM: "Khảo thí",
  PM: "Thực tập",
};

export function StatusBadge({ status }: { status: SyncStatus }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-2 py-0.5 text-[0.625rem] font-medium",
        status === "SUCCESS"
          ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400"
          : "border-destructive/30 bg-destructive/10 text-destructive",
      )}
    >
      {status === "SUCCESS" ? "Thành công" : "Lỗi"}
    </span>
  );
}

/** "4 bản ghi (mới 1, cập nhật 2, bỏ qua 1)". */
export function describeCounts(counts: SyncCounts | null): string {
  if (!counts) return "—";
  const parts = [
    counts.inserted ? `mới ${counts.inserted}` : null,
    counts.updated ? `cập nhật ${counts.updated}` : null,
    counts.skipped ? `bỏ qua ${counts.skipped}` : null,
  ].filter(Boolean);
  return `${counts.fetched} bản ghi${parts.length ? ` (${parts.join(", ")})` : ""}`;
}
