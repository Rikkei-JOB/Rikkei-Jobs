import { createContext, useContext } from "react";
import type { StaffJobStatus } from "@/interface/staff-job";

/** Đường dẫn gốc của màn Duyệt JD: /staff/jobs cho cán bộ, /admin/jobs cho quản trị (cùng component). */
export const JobsBase = createContext("/staff/jobs");
export const useJobsBase = () => useContext(JobsBase);

const DAY = 86_400_000;
const ZONE = "Asia/Ho_Chi_Minh";

export const STATUS_INFO: Record<StaffJobStatus, { label: string; tone: string }> = {
  PENDING: { label: "Chờ duyệt", tone: "pending" },
  APPROVED: { label: "Đang tuyển", tone: "approved" },
  REJECTED: { label: "Bị trả lại", tone: "rejected" },
  CLOSED: { label: "Đã đóng", tone: "closed" },
};

const todayKey = (now: number) => new Intl.DateTimeFormat("en-CA", { timeZone: ZONE }).format(new Date(now));

/** Số ngày từ hôm nay (giờ Việt Nam) tới hạn nộp yyyy-MM-dd; âm là đã quá hạn. */
export function daysLeft(deadline: string, now: number) {
  return Math.round((Date.parse(`${deadline}T00:00:00Z`) - Date.parse(`${todayKey(now)}T00:00:00Z`)) / DAY);
}

export function deadlineText(deadline: string | null, now: number) {
  if (!deadline) return { text: "Chưa có hạn nộp", tone: "muted" };
  const days = daysLeft(deadline, now);
  if (days < 0) return { text: `Quá hạn ${-days} ngày`, tone: "danger" };
  if (days === 0) return { text: "Hết hạn hôm nay", tone: "danger" };
  return { text: `Còn ${days} ngày`, tone: days <= 7 ? "warn" : "muted" };
}

/** "5 phút trước", "3 giờ trước", "2 ngày trước". */
export function timeAgo(iso: string, now: number) {
  const minutes = Math.max(0, Math.floor((now - Date.parse(iso)) / 60_000));
  if (minutes < 1) return "Vừa xong";
  if (minutes < 60) return `${minutes} phút trước`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} giờ trước`;
  return `${Math.floor(hours / 24)} ngày trước`;
}

export function formatDateTime(iso: string) {
  return new Intl.DateTimeFormat("vi-VN", { timeZone: ZONE, dateStyle: "short", timeStyle: "short" }).format(new Date(iso));
}
