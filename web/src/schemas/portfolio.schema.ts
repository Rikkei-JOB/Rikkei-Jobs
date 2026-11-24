import { z } from "zod";
import type { PortfolioPayload } from "@/interface/portfolio";
import { PROFILE_FILE_LIMIT } from "@/schemas/profile-qualifications.schema";

export const PORTFOLIO_MAX_IMAGES = 6;
// Cùng luật với backend: chỉ http(s), phải có host.
const URL_PATTERN = /^https?:\/\/[^\s/?#]+([/?#]\S*)?$/i;
const optionalUrl = z.string().trim().max(1000, "URL tối đa 1.000 ký tự")
  .refine(value => !value || URL_PATTERN.test(value), "Phải là URL http(s) hợp lệ, ví dụ https://github.com/...");

export const portfolioSchema = z.object({
  name: z.string().trim().min(1, "Vui lòng nhập tên dự án").max(255, "Tên dự án tối đa 255 ký tự"),
  role: z.string().trim().max(255, "Vai trò tối đa 255 ký tự"),
  description: z.string().trim().max(5000, "Mô tả tối đa 5.000 ký tự"),
  techStack: z.string().refine(value => splitTags(value).length <= 20, "Tối đa 20 công nghệ")
    .refine(value => splitTags(value).every(tag => tag.length <= 50), "Mỗi công nghệ tối đa 50 ký tự"),
  repoUrl: optionalUrl, demoUrl: optionalUrl,
  startDate: z.string(), endDate: z.string(),
  visible: z.boolean(),
}).refine(value => !value.startDate || !value.endDate || value.endDate >= value.startDate,
  { path: ["endDate"], message: "Ngày kết thúc phải từ ngày bắt đầu trở đi" });
export type PortfolioValues = z.infer<typeof portfolioSchema>;

export function splitTags(value: string): string[] {
  return [...new Set(value.split(",").map(tag => tag.trim()).filter(Boolean))];
}

export function portfolioPayload(values: PortfolioValues): PortfolioPayload {
  return { name: values.name, role: values.role || null, description: values.description || null,
    techStack: splitTags(values.techStack), repoUrl: values.repoUrl || null, demoUrl: values.demoUrl || null,
    startDate: values.startDate || null, endDate: values.endDate || null, visible: values.visible };
}

export function validatePortfolioImage(file: File): string | null {
  if (!file.size) return `${file.name}: tệp rỗng`;
  if (file.size > PROFILE_FILE_LIMIT) return `${file.name}: ảnh vượt quá 5MB`;
  if (!["image/jpeg", "image/png"].includes(file.type)) return `${file.name}: chỉ nhận JPG hoặc PNG`;
  return null;
}
