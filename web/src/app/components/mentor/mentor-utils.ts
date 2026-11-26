import type { MentorRoundItem, RoundResult } from "@/interface/hr-application";

const ZONE = "Asia/Ho_Chi_Minh";
const DAY = 86_400_000;

export const RESULT_INFO: Record<RoundResult, { label: string; tone: "pending" | "pass" | "fail" }> = {
  PENDING: { label: "Chờ kết quả", tone: "pending" },
  PASS: { label: "Đạt", tone: "pass" },
  FAIL: { label: "Không đạt", tone: "fail" },
};

/**
 * Trạng thái hiển thị của một vòng với mentor:
 * review = đã tới giờ, chưa chấm; upcoming = chưa tới giờ; unscheduled = chưa hẹn lịch;
 * done = đã có kết quả; locked = đơn đã rời bước phỏng vấn nên không sửa được nữa.
 */
export type RoundStage = "review" | "upcoming" | "unscheduled" | "done" | "locked";

export function stageOf(item: MentorRoundItem, now = Date.now()): RoundStage {
  if (item.round.result !== "PENDING") return item.applicationStatus === "INTERVIEW" ? "done" : "locked";
  if (item.applicationStatus !== "INTERVIEW") return "locked";
  if (!item.round.scheduledAt) return "unscheduled";
  return Date.parse(item.round.scheduledAt) <= now ? "review" : "upcoming";
}

export const canEdit = (item: MentorRoundItem) => item.applicationStatus === "INTERVIEW";

/** yyyy-MM-dd theo giờ Việt Nam, dùng để nhóm lịch theo ngày. */
export function dayKey(value: string | Date) {
  return new Intl.DateTimeFormat("en-CA", { timeZone: ZONE, year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date(value));
}

export function formatTime(value: string) {
  return new Intl.DateTimeFormat("vi-VN", { timeZone: ZONE, hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}

/** "Thứ Năm, 27/11" */
export function formatDayLabel(value: string | Date) {
  const text = new Intl.DateTimeFormat("vi-VN", { timeZone: ZONE, weekday: "long", day: "2-digit", month: "2-digit" }).format(new Date(value));
  return text.charAt(0).toLocaleUpperCase("vi-VN") + text.slice(1);
}

export function dateParts(value: string | Date) {
  const date = new Date(value);
  const day = new Intl.DateTimeFormat("vi-VN", { timeZone: ZONE, day: "2-digit" }).format(date);
  const month = new Intl.DateTimeFormat("vi-VN", { timeZone: ZONE, month: "2-digit" }).format(date);
  const weekday = new Intl.DateTimeFormat("vi-VN", { timeZone: ZONE, weekday: "short" }).format(date);
  return { day, month, weekday };
}

/** "Hôm nay", "Ngày mai", "Còn 3 ngày", "2 ngày trước" theo ngày lịch Việt Nam. */
export function relativeDay(value: string, now = Date.now()) {
  const diff = Math.round((Date.parse(`${dayKey(value)}T00:00:00Z`) - Date.parse(`${dayKey(new Date(now))}T00:00:00Z`)) / DAY);
  if (diff === 0) return "Hôm nay";
  if (diff === 1) return "Ngày mai";
  if (diff === -1) return "Hôm qua";
  return diff > 0 ? `Còn ${diff} ngày` : `${-diff} ngày trước`;
}

/** Xếp loại theo điểm 0–100 để mentor thấy ngay mức đánh giá khi kéo thanh điểm. */
export function scoreBand(score: number) {
  if (score >= 85) return { label: "Xuất sắc", tone: "excellent" as const };
  if (score >= 70) return { label: "Tốt", tone: "good" as const };
  if (score >= 50) return { label: "Đạt yêu cầu", tone: "fair" as const };
  return { label: "Chưa đạt", tone: "weak" as const };
}

export function byScheduleAsc(a: MentorRoundItem, b: MentorRoundItem) {
  return (a.round.scheduledAt ?? "9999").localeCompare(b.round.scheduledAt ?? "9999");
}
