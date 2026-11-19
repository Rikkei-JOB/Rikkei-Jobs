import { ApiError, getErrorMessage } from "@/services/api";
import type { ChangeEntity, DataReportStatus, WeightSource } from "@/interface/admin";
import { cn } from "cn";

export { formatDateTime } from "@/app/components/sync/sync-format";

export const WEIGHT_SOURCE_LABEL: Record<WeightSource, string> = {
  LMS: "Học tập (LMS)",
  EXAM: "Thi (Khảo thí)",
  INTERNSHIP: "Thực tập",
  MENTOR: "Mentor đánh giá",
};

export const ENTITY_LABEL: Record<ChangeEntity, string> = {
  SKILL: "Kỹ năng",
  SCORE_WEIGHT: "Trọng số",
  EVALUATION_TEMPLATE: "Mẫu phiếu",
  PROFILE: "Hồ sơ",
  EVIDENCE: "Bằng chứng",
  DATA_REPORT: "Báo sai",
};

export const REPORT_STATUS_LABEL: Record<DataReportStatus, string> = {
  OPEN: "Chờ xử lý",
  RESOLVED: "Đã xử lý",
  REJECTED: "Từ chối",
};

export function ReportStatusBadge({ status }: { status: DataReportStatus }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-md border px-1.5 py-0.5 text-[11px] font-medium whitespace-nowrap",
        status === "OPEN" && "border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-800 dark:bg-amber-950/60 dark:text-amber-300",
        status === "RESOLVED" && "border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300",
        status === "REJECTED" && "border-border bg-muted text-muted-foreground",
      )}
    >
      {REPORT_STATUS_LABEL[status]}
    </span>
  );
}

/** Lỗi theo trường từ 400 VALIDATION_ERROR ({ errors: { field: message } }). */
export function fieldError(error: unknown, field: string): string | undefined {
  if (error instanceof ApiError && error.errors && typeof error.errors === "object") {
    const value = (error.errors as Record<string, unknown>)[field];
    return typeof value === "string" ? value : undefined;
  }
  return undefined;
}

/** Thông báo lỗi chung; nếu chỉ có một lỗi theo trường thì hiện luôn lỗi đó cho dễ hiểu. */
export function errorText(error: unknown): string {
  if (error instanceof ApiError && error.errors && typeof error.errors === "object") {
    const messages = Object.values(error.errors as Record<string, unknown>).filter((v) => typeof v === "string");
    if (messages.length > 0) return messages.join(". ");
  }
  return getErrorMessage(error);
}

export const textareaClass =
  "w-full min-w-0 rounded-md border border-input bg-input/20 px-2 py-1.5 text-xs/relaxed outline-none transition-colors placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 dark:bg-input/30";

export const selectClass =
  "h-7 rounded-md border border-input bg-input/20 px-2 text-xs/relaxed outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 dark:bg-input/30";
