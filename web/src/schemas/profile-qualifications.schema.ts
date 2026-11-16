import { z } from "zod";
import type { CertificatePayload, EducationPayload } from "@/interface/profile-qualifications";

const date = z.string().refine(value => {
  const parsed = new Date(`${value}T00:00:00Z`);
  return /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
}, "Ngày không hợp lệ");
const optionalDate = z.union([z.literal(""), date]);
export const educationSchema = z.object({
  school: z.string().trim().min(1, "Vui lòng nhập trường học").max(255, "Trường học tối đa 255 ký tự"),
  major: z.string().trim().max(255, "Chuyên ngành tối đa 255 ký tự"),
  fromDate: date, toDate: optionalDate,
  gpa: z.string().trim().refine(value => !value || (Number.isFinite(Number(value)) && Number(value) >= 0 && Number(value) <= 10), "GPA phải từ 0 đến 10"),
  note: z.string().trim().max(5000, "Ghi chú tối đa 5.000 ký tự"),
}).refine(value => !value.toDate || value.toDate >= value.fromDate, { path: ["toDate"], message: "Ngày kết thúc phải từ ngày bắt đầu trở đi" });
export type EducationValues = z.infer<typeof educationSchema>;
export function educationPayload(values: EducationValues): EducationPayload {
  return { ...values, major: values.major || null, toDate: values.toDate || null,
    gpa: values.gpa === "" ? null : Number(values.gpa), note: values.note || null };
}

export const certificateSchema = z.object({
  name: z.string().trim().min(1, "Vui lòng nhập tên chứng chỉ").max(255, "Tên chứng chỉ tối đa 255 ký tự"),
  issuer: z.string().trim().min(1, "Vui lòng nhập đơn vị cấp").max(255, "Đơn vị cấp tối đa 255 ký tự"),
  issuedAt: date, expiresAt: optionalDate,
}).refine(value => !value.expiresAt || value.expiresAt >= value.issuedAt, { path: ["expiresAt"], message: "Ngày hết hạn phải từ ngày cấp trở đi" });
export type CertificateValues = z.infer<typeof certificateSchema>;
export function certificatePayload(values: CertificateValues, fileUrl: string | null): CertificatePayload {
  return { ...values, expiresAt: values.expiresAt || null, fileUrl };
}

export const PROFILE_FILE_LIMIT = 5 * 1024 * 1024;
export function validateProfileFile(file: File, avatar = false): string | null {
  if (!file.size) return "Tệp không được rỗng";
  if (file.size > PROFILE_FILE_LIMIT) return "Tệp vượt quá 5MB";
  const allowed = avatar ? ["image/jpeg", "image/png"] : ["application/pdf", "image/jpeg", "image/png"];
  if (!allowed.includes(file.type)) return avatar ? "Ảnh đại diện chỉ nhận JPG hoặc PNG" : "Chỉ nhận PDF, JPG hoặc PNG";
  return null;
}
