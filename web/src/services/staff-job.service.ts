import { api } from "@/services/api";
import type { PageResult } from "@/interface/company";
import type { StaffJobDetail, StaffJobItem, StaffJobListParams } from "@/interface/staff-job";

/** DN-07: các endpoint /api/v1/staff/jobs/... để cán bộ (STAFF) duyệt, trả lại, đóng JD. */
export const staffJobService = {
  list: async ({ status, companyId, page, size }: StaffJobListParams) =>
    (
      await api.get<PageResult<StaffJobItem>>("/staff/jobs", {
        params: { status, companyId: companyId || undefined, page, size },
      })
    ).data,

  get: async (id: string) => (await api.get<StaffJobDetail>(`/staff/jobs/${id}`)).data,

  /** Chỉ JD PENDING. 409 INVALID_JOB_STATUS / JOB_EXPIRED / CONFLICT. */
  approve: async (id: string) => (await api.post<StaffJobDetail>(`/staff/jobs/${id}/approve`)).data,

  /** Chỉ JD PENDING; lý do bắt buộc (400 errors.reason). */
  reject: async (id: string, reason: string) =>
    (await api.post<StaffJobDetail>(`/staff/jobs/${id}/reject`, { reason })).data,

  /** Chỉ JD APPROVED; lý do bắt buộc (400 errors.reason). */
  close: async (id: string, reason: string) =>
    (await api.post<StaffJobDetail>(`/staff/jobs/${id}/close`, { reason })).data,
};
