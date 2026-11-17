import { api } from "@/services/api";
import type { ApplicationStatus } from "@/interface/application";
import type { HrApplicationDetail, HrApplicationPage, InterviewRound, RoundResult } from "@/interface/hr-application";

const root = "/recruitment";
const idPath = (id: string) => `${root}/applications/${encodeURIComponent(id)}`;
export const hrApplicationService = {
  async listHr(params: { jobId?: string; status?: ApplicationStatus; page?: number; size?: number }) {
    return (await api.get<HrApplicationPage>(`${root}/hr/applications`, { params })).data;
  },
  async listStaff(params: { companyId?: string; jobId?: string; status?: ApplicationStatus; page?: number; size?: number }) {
    return (await api.get<HrApplicationPage>(`${root}/staff/applications`, { params })).data;
  },
  async detail(id: string) { return (await api.get<HrApplicationDetail>(idPath(id))).data; },
  async transition(id: string, toStatus: ApplicationStatus, note?: string) {
    return (await api.post(`${idPath(id)}/transition`, { toStatus, note: note || null })).data;
  },
  async createRound(id: string, payload: { name: string; interviewerEmail: string; scheduledAt?: string | null }) {
    return (await api.post<InterviewRound>(`${idPath(id)}/rounds`, payload)).data;
  },
  async updateRound(id: string, roundId: string, payload: { result: RoundResult; score: number | null; note: string | null }) {
    return (await api.put<InterviewRound>(`${idPath(id)}/rounds/${encodeURIComponent(roundId)}`, payload)).data;
  },
  async downloadCv(id: string) {
    return (await api.get<Blob>(`${idPath(id)}/cv`, { responseType: "blob" })).data;
  },
};
