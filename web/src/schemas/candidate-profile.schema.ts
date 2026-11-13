import { z } from "zod";
import type { UpdateCandidateProfile } from "@/interface/candidate-profile";

export function localToday() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

export const candidateProfileSchema = z.object({
  fullName: z.string().trim().min(1, "Vui lòng nhập họ và tên").max(255, "Họ và tên tối đa 255 ký tự"),
  email: z.string().trim().email("Email không hợp lệ").max(255, "Email tối đa 255 ký tự"),
  dob: z.string().refine(value => {
    if (!value) return true;
    const parsed = new Date(`${value}T00:00:00Z`);
    return /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
  }, "Ngày sinh không hợp lệ").refine(value => !value || value <= localToday(), "Ngày sinh không được ở tương lai"),
  gender: z.union([z.literal(""), z.enum(["MALE", "FEMALE", "OTHER"])]),
  phone: z.string().trim().refine(value => !value || /^0[0-9]{9}$/.test(value), "Số điện thoại phải gồm 10 số và bắt đầu bằng 0"),
  address: z.string().trim().max(500, "Địa chỉ tối đa 500 ký tự"),
  headline: z.string().trim().max(255, "Tiêu đề tối đa 255 ký tự"),
  summary: z.string().trim().max(5000, "Giới thiệu tối đa 5.000 ký tự"),
});
export type CandidateProfileValues = z.infer<typeof candidateProfileSchema>;

export function toProfilePayload(values: CandidateProfileValues): UpdateCandidateProfile {
  return {
    fullName: values.fullName, email: values.email, dob: values.dob || null, gender: values.gender || null,
    phone: values.phone || null, address: values.address || null, headline: values.headline || null, summary: values.summary || null,
  };
}
