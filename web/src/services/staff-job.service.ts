import { api } from "@/services/api";
import type { PageResult } from "@/interface/company";
import type { StaffJobDetail, StaffJobItem, StaffJobStatus, StaffJobSummary } from "@/interface/staff-job";

/** DN-07: cán bộ duyệt / trả lại JD (company-service /staff/jobs). */
export const staffJobService = {
  list: async (params: { status?: StaffJobStatus; q?: string; page?: number; size?: number }) =>
    (await api.get<PageResult<StaffJobItem>>("/staff/jobs", { params: { ...params, q: params.q || undefined } })).data,
  summary: async () => (await api.get<StaffJobSummary>("/staff/jobs/summary")).data,
  get: async (id: string) => (await api.get<StaffJobDetail>(`/staff/jobs/${encodeURIComponent(id)}`)).data,
  approve: async (id: string) => (await api.post<StaffJobDetail>(`/staff/jobs/${encodeURIComponent(id)}/approve`)).data,
  reject: async (id: string, reason: string) => (await api.post<StaffJobDetail>(`/staff/jobs/${encodeURIComponent(id)}/reject`, { reason })).data,
};
