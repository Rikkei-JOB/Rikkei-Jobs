import type { JobLevel, JobStatus, JobType, WorkMode } from "@/interface/job";

export const LEVEL_LABEL: Record<JobLevel, string> = {
  INTERN: "Intern",
  FRESHER: "Fresher",
  JUNIOR: "Junior",
  MIDDLE: "Middle",
  SENIOR: "Senior",
};

export const TYPE_LABEL: Record<JobType, string> = {
  FULLTIME: "Toàn thời gian",
  PARTTIME: "Bán thời gian",
  INTERNSHIP: "Thực tập",
};

export const WORK_MODE_LABEL: Record<WorkMode, string> = {
  ONSITE: "Tại văn phòng",
  REMOTE: "Từ xa",
  HYBRID: "Hybrid",
};

export const STATUS_LABEL: Record<JobStatus, string> = {
  DRAFT: "Nháp",
  PENDING: "Chờ duyệt",
  APPROVED: "Đang tuyển",
  REJECTED: "Bị trả lại",
  CLOSED: "Đã đóng",
};

/** Màu nhãn trạng thái: Nháp xám, Chờ duyệt vàng, Đang tuyển xanh lá, Bị trả lại đỏ, Đã đóng xám đậm. */
export const STATUS_CLASS: Record<JobStatus, string> = {
  DRAFT: "bg-gray-100 text-gray-600",
  PENDING: "bg-yellow-100 text-yellow-800",
  APPROVED: "bg-green-100 text-green-700",
  REJECTED: "bg-red-100 text-red-700",
  CLOSED: "bg-gray-300 text-gray-800",
};

export const STATUS_ORDER: JobStatus[] = ["DRAFT", "PENDING", "APPROVED", "REJECTED", "CLOSED"];

/** yyyy-MM-dd hoặc ISO instant -> dd/MM/yyyy (theo giờ Việt Nam khi là instant); null -> "—". */
export function formatDate(value: string | null): string {
  if (!value) return "—";
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [y, m, d] = value.split("-");
    return `${d}/${m}/${y}`;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}
