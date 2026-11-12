import type { ApplicationStatus } from "@/interface/application";

export const APPLICATION_STATUS: Record<ApplicationStatus, { label: string; className: string }> = {
  SUBMITTED: { label: "Đã gửi", className: "border-blue-200 bg-blue-50 text-blue-800" },
  SCREENING: { label: "Sàng lọc", className: "border-amber-200 bg-amber-50 text-amber-800" },
  INTERVIEW: { label: "Phỏng vấn", className: "border-violet-200 bg-violet-50 text-violet-800" },
  OFFER: { label: "Offer", className: "border-cyan-200 bg-cyan-50 text-cyan-800" },
  HIRED: { label: "Nhận việc", className: "border-emerald-200 bg-emerald-50 text-emerald-800" },
  REJECTED: { label: "Không đạt", className: "border-red-200 bg-red-50 text-red-800" },
  WITHDRAWN: { label: "Đã rút", className: "border-slate-200 bg-slate-100 text-slate-700" },
};
export const APPLICATION_STEPS: ApplicationStatus[] = ["SUBMITTED", "SCREENING", "INTERVIEW", "OFFER", "HIRED"];
export function isApplicationStatus(value: string | null): value is ApplicationStatus {
  return value !== null && Object.hasOwn(APPLICATION_STATUS, value);
}
const jobsRoot = (process.env.NEXT_PUBLIC_JOB_PAGE_PATH ?? "/jobs").replace(/\/$/, "");
export const applicationLinks = {
  cvs: process.env.NEXT_PUBLIC_CV_PAGE_PATH ?? "/candidate/cvs",
  jobs: jobsRoot,
  job: (id: string) => `${jobsRoot}/${encodeURIComponent(id)}`,
};
export function formatApplicationDate(value: string) {
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short", timeZone: "Asia/Ho_Chi_Minh" }).format(new Date(value));
}
