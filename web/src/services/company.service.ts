import { api } from "@/services/api";
import { env } from "@/config/env";
import type { Company, UpdateCompanyPayload } from "@/interface/company";

/** Các endpoint /api/v1/companies/... dành cho HR (công ty của chính mình). */
export const companyService = {
  getMine: async () => (await api.get<Company>("/companies/me")).data,

  updateMine: async (payload: UpdateCompanyPayload) => (await api.put<Company>("/companies/me", payload)).data,

  uploadLogo: async (file: File) => {
    const form = new FormData();
    form.append("file", file);
    // Ghi đè Content-Type mặc định (application/json) để axios tự thêm boundary cho multipart.
    return (await api.post<Company>("/companies/me/logo", form, { headers: { "Content-Type": "multipart/form-data" } }))
      .data;
  },

  /** logoUrl backend trả là đường dẫn tương đối ("/companies/logos/<uuid>.png"). */
  logoSrc: (logoUrl: string | null) => (logoUrl ? env.apiUrl + logoUrl : null),
};
