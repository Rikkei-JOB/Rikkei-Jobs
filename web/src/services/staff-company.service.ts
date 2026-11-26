import { api } from "@/services/api";
import type {
  Company,
  CompanyActionItem,
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

  /** DN-07: khoá công ty, lý do bắt buộc (400 errors.reason); đã khoá → 409; identity lỗi → 503. */
  lock: async (id: string, reason: string) =>
    (await api.post<StaffCompanyDetail>(`/staff/companies/${id}/lock`, { reason })).data,

  /** DN-07: mở khoá công ty (không có body); đang hoạt động → 409; identity lỗi → 503. */
  unlock: async (id: string) => (await api.post<StaffCompanyDetail>(`/staff/companies/${id}/unlock`)).data,

  /** DN-07: lịch sử thao tác trên công ty, mới nhất trước; page từ 1. */
  history: async (id: string, { page, size }: { page: number; size?: number }) =>
    (await api.get<PageResult<CompanyActionItem>>(`/staff/companies/${id}/history`, { params: { page, size } })).data,

  createHr: async (companyId: string, body: { email: string; fullName: string }) =>
    (await api.post<StaffHrAccount>(`/staff/companies/${companyId}/hr-accounts`, body)).data,
};
