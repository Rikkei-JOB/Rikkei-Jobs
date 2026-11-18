import { api } from "@/services/api";
import type {
  Company,
  CreateCompanyPayload,
  PageResult,
  StaffCompanyDetail,
  StaffCompanyItem,
  StaffCompanyListParams,
  StaffHrAccount,
} from "@/interface/company";

/** Các endpoint /api/v1/staff/... dành cho cán bộ (STAFF). */
export const staffCompanyService = {
  list: async (params: StaffCompanyListParams) => {
    const query = { ...params, q: params.q?.trim() || undefined };
    return (await api.get<PageResult<StaffCompanyItem>>("/staff/companies", { params: query })).data;
  },

  detail: async (id: string) => (await api.get<StaffCompanyDetail>(`/staff/companies/${id}`)).data,

  /** DN-01: cán bộ tạo công ty (ACTIVE). Trùng mã số thuế → 409. */
  createCompany: async (body: CreateCompanyPayload) => (await api.post<Company>("/staff/companies", body)).data,

  lockHr: async (userId: string, reason: string) =>
    (await api.post<StaffHrAccount>(`/staff/hr-accounts/${userId}/lock`, { reason })).data,

  unlockHr: async (userId: string) => (await api.post<StaffHrAccount>(`/staff/hr-accounts/${userId}/unlock`)).data,

  createHr: async (companyId: string, body: { email: string; fullName: string }) =>
    (await api.post<StaffHrAccount>(`/staff/companies/${companyId}/hr-accounts`, body)).data,
};
