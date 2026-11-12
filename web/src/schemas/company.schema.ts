import { z } from "zod";
import type { UpdateCompanyPayload } from "@/interface/company";

export const COMPANY_SIZES = ["1-50", "51-200", "201-1000", "1000+"] as const;
export const LOGO_MAX_BYTES = 2 * 1024 * 1024;

export const companyProfileSchema = z.object({
  name: z.string().trim().min(1, "Vui lòng nhập tên công ty").max(255, "Tên công ty tối đa 255 ký tự"),
  industry: z.string().trim().max(100, "Ngành tối đa 100 ký tự"),
  size: z.union([z.literal(""), z.enum(COMPANY_SIZES)]),
  address: z.string().trim().max(255, "Địa chỉ tối đa 255 ký tự"),
  city: z.string().trim().max(100, "Thành phố tối đa 100 ký tự"),
  website: z
    .string()
    .trim()
    .max(255, "Website tối đa 255 ký tự")
    .refine((v) => v === "" || /^https?:\/\/\S+$/.test(v), "Website phải bắt đầu bằng http:// hoặc https://"),
  description: z.string().trim(),
});
export type CompanyProfileValues = z.infer<typeof companyProfileSchema>;

const orNull = (value: string) => {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
};

/** Chuỗi rỗng (sau trim) -> null để backend lưu "chưa có". */
export function toUpdatePayload(values: CompanyProfileValues): UpdateCompanyPayload {
  return {
    name: values.name.trim(),
    industry: orNull(values.industry),
    size: values.size === "" ? null : values.size,
    address: orNull(values.address),
    city: orNull(values.city),
    website: orNull(values.website),
    description: orNull(values.description),
  };
}

/** Kiểm tra sơ bộ ở trình duyệt; backend vẫn kiểm tra lại. Trả message lỗi hoặc null. */
export function validateLogoFile(file: File): string | null {
  if (file.type !== "image/jpeg" && file.type !== "image/png") {
    return "Chỉ chấp nhận ảnh JPG hoặc PNG";
  }
  if (file.size > LOGO_MAX_BYTES) {
    return "Ảnh vượt quá 2MB";
  }
  return null;
}
