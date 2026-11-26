/**
 * Tiện ích hiển thị dùng chung cho các phần của hồ sơ ứng viên.
 * Class theo DESIGN.md (đỏ trầm, control 44px). Theme chung (`globals.css`, Button) còn đen/trắng nên giá trị
 * tạm viết thẳng ở đây, cùng giá trị với `--career-*` trong career.css.
 * shortcut: mã màu lặp lại career.css; khi team đưa token vào globals.css thì thay bằng `bg-primary`/`text-heading`.
 */

/** "2024-03-01" → "03/2024"; giữ nguyên nếu không đúng dạng ISO. */
export function monthYear(date: string) {
  const [y, m] = date.split("-");
  return y && m ? `${m}/${y}` : date;
}

/** Chữ cái đầu của 2 từ đầu tiên, ví dụ "Rikkei Jobs" → "RJ". */
export function initials(name: string) {
  return name.trim().split(/\s+/).slice(0, 2).map(w => w[0]?.toUpperCase() ?? "").join("") || "?";
}

/** Nền nhấn nhẹ cho avatar, ô chữ viết tắt, ảnh bìa trống. */
export const BRAND_SOFT = "bg-[#FFF1F2] text-[#B4232D]";
export const SECTION_TITLE = "text-xl font-bold text-[#051A53]";
export const ITEM_TITLE = "text-lg leading-[26px] font-bold text-[#051A53] break-words";
export const META = "text-xs leading-[18px] text-[#64748B]";

/** Nút hành động chính của mỗi khu vực. */
export const PRIMARY_CTA = "min-h-11 gap-2 px-4 text-sm bg-[#B4232D] text-white hover:bg-[#941C25] [&_svg:not([class*='size-'])]:size-4";
export const SECONDARY_CTA = "min-h-11 gap-2 px-4 text-sm border-[#64748B] bg-white text-[#334155] hover:bg-[#F6F7FA] [&_svg:not([class*='size-'])]:size-4";
/** Nút icon sửa/xoá: vùng bấm 44px, icon 20px. */
export const ICON_ACTION = "size-11 bg-white text-[#475569] hover:bg-[#FFF1F2] hover:text-[#B4232D] [&_svg:not([class*='size-'])]:size-5";
export const ICON_DANGER = "size-11 bg-white text-[#B91C1C] hover:bg-[#FEF2F2] [&_svg:not([class*='size-'])]:size-5";

/** Badge trạng thái theo màu ngữ nghĩa của DESIGN.md. */
export const BADGE = "inline-flex w-fit items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium";
export const TONE = {
  success: "bg-[#F0FDF4] text-[#166534]",
  warning: "bg-[#FFFBEB] text-[#92400E]",
  danger: "bg-[#FEF2F2] text-[#B91C1C]",
  info: "bg-[#EFF6FF] text-[#1D4ED8]",
  neutral: "bg-slate-100 text-[#475569]",
} as const;
export const CHIP = "rounded-full bg-slate-100 px-2.5 py-0.5 text-xs text-[#334155]";

/** Card con trong khu vực: viền nhẹ, bóng chuẩn; rê chuột viền đỏ nhạt, nhấc nhẹ nếu không giảm chuyển động. */
export const ITEM_CARD = "rounded-xl border border-[#E2E8F0] bg-white shadow-[0_2px_8px_rgb(15_23_42/0.04)] transition duration-200 hover:border-[#B4232D]/40 motion-safe:hover:-translate-y-0.5";
/** Ô nét đứt cho trạng thái rỗng / thêm mới. */
export const ADD_TILE = "flex w-full flex-col items-center justify-center gap-1 rounded-xl border border-dashed border-[#CBD5E1] p-6 text-[#64748B] transition-colors duration-200 hover:border-[#B4232D] hover:text-[#B4232D]";

/** Màu nền cố định theo chuỗi để mỗi mục có màu riêng mà không phải lưu (portfolio, chứng chỉ còn dùng). */
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
