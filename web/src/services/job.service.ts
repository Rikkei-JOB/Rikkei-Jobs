import { api } from "@/services/api";
import type { PageResult } from "@/interface/company";
import type { Job, JobListParams, JobPayload, JobStatusSummary, JobSummary } from "@/interface/job";

/** Các endpoint JD của company-service dành cho HR. */
export const jobService = {
  listMine: async (params: JobListParams, signal?: AbortSignal) =>
    (await api.get<PageResult<JobSummary>>("/companies/me/jobs", { params: { size: 20, ...params }, signal })).data,

  summaryMine: async () => (await api.get<JobStatusSummary>("/companies/me/jobs/summary")).data,

  get: async (id: string, signal?: AbortSignal) => (await api.get<Job>(`/jobs/${encodeURIComponent(id)}`, { signal })).data,

  create: async (payload: JobPayload) => (await api.post<Job>("/jobs", payload)).data,

  update: async (id: string, payload: JobPayload) => (await api.put<Job>(`/jobs/${id}`, payload)).data,

  submit: async (id: string) => (await api.post<Job>(`/jobs/${id}/submit`)).data,

  close: async (id: string) => (await api.post<Job>(`/jobs/${id}/close`)).data,
};
