/** Tiện ích hiển thị dùng chung cho các phần của hồ sơ. */

/** "2024-03-01" → "03/2024"; giữ nguyên nếu không đúng dạng ISO. */
export function monthYear(date: string) {
  const [y, m] = date.split("-");
  return y && m ? `${m}/${y}` : date;
}

/** Chữ cái đầu của 2 từ đầu tiên, ví dụ "Rikkei Jobs" → "RJ". */
export function initials(name: string) {
  return name.trim().split(/\s+/).slice(0, 2).map(w => w[0]?.toUpperCase() ?? "").join("") || "?";
}

/** Màu nền cố định theo chuỗi để mỗi mục có màu riêng mà không phải lưu. */
const TONES = [
  "bg-violet-100 text-violet-800 dark:bg-violet-950 dark:text-violet-200",
  "bg-teal-100 text-teal-800 dark:bg-teal-950 dark:text-teal-200",
  "bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-200",
  "bg-sky-100 text-sky-800 dark:bg-sky-950 dark:text-sky-200",
  "bg-rose-100 text-rose-800 dark:bg-rose-950 dark:text-rose-200",
  "bg-lime-100 text-lime-800 dark:bg-lime-950 dark:text-lime-200",
];
export function toneOf(key: string) {
  let hash = 0;
  for (const ch of key) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
  return TONES[hash % TONES.length];
}

/** Hàng nút sửa/xoá: ẩn tới khi rê chuột hoặc focus vào mục cha có class `group`; màn cảm ứng luôn hiện. */
export const HOVER_ACTIONS = "flex shrink-0 gap-1 opacity-0 transition-opacity group-hover:opacity-100 group-focus-within:opacity-100 pointer-coarse:opacity-100";
