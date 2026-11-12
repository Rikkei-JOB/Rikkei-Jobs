import { api } from "@/services/api";
import type { ApplicationDetail, ApplicationPage, ApplicationStatus, ApplyPayload } from "@/interface/application";

const own = "/recruitment/me/applications";
export const applicationService = {
  async apply(jobId: string, payload: ApplyPayload): Promise<ApplicationDetail> {
    return (await api.post<ApplicationDetail>(`/recruitment/jobs/${encodeURIComponent(jobId)}/apply`, payload)).data;
  },
  async list(params: { status?: ApplicationStatus; page?: number; size?: number } = {}): Promise<ApplicationPage> {
    return (await api.get<ApplicationPage>(own, { params })).data;
  },
  async detail(id: string): Promise<ApplicationDetail> {
    return (await api.get<ApplicationDetail>(`${own}/${encodeURIComponent(id)}`)).data;
  },
  async withdraw(id: string, payload: { reason: string }): Promise<ApplicationDetail> {
    return (await api.post<ApplicationDetail>(`${own}/${encodeURIComponent(id)}/withdraw`, payload)).data;
  },
  async downloadSnapshot(id: string): Promise<Blob> {
    return (await api.get<Blob>(`/recruitment/applications/${encodeURIComponent(id)}/cv`, { responseType: "blob" })).data;
  },
};
