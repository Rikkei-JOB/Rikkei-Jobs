import { api } from "@/services/api";
import type { ApplicationStatus } from "@/interface/application";
import type { HrApplicationDetail, HrApplicationPage, InterviewRound, MentorRoundPage, RoundResult, MatchLookupResponse } from "@/interface/hr-application";
import type { CreateOfferPayload, EmploymentRecord, Offer } from "@/interface/offer";

const root = "/recruitment";
const idPath = (id: string) => `${root}/applications/${encodeURIComponent(id)}`;
export const hrApplicationService = {
  async listHr(params: { jobId?: string; status?: ApplicationStatus; page?: number; size?: number }) {
    return (await api.get<HrApplicationPage>(`${root}/hr/applications`, { params })).data;
  },
  async listStaff(params: { companyId?: string; jobId?: string; status?: ApplicationStatus; page?: number; size?: number }) {
    return (await api.get<HrApplicationPage>(`${root}/staff/applications`, { params })).data;
  },
  async detail(id: string, signal?: AbortSignal) { return (await api.get<HrApplicationDetail>(idPath(id), { signal })).data; },
  async match(id: string, signal?: AbortSignal) { return (await api.get<MatchLookupResponse>(`${idPath(id)}/match`, { signal })).data; },
  async sendOffer(id: string, payload: CreateOfferPayload) {
    return (await api.post<Offer>(`${idPath(id)}/offer`, payload)).data;
  },
  async confirmOnboard(id: string) {
    return (await api.post<EmploymentRecord>(`${idPath(id)}/offer/confirm-onboard`)).data;
  },
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
  async listMentorRounds(params: { page?: number; size?: number } = {}) {
    return (await api.get<MentorRoundPage>(`${root}/mentor/rounds`, { params })).data;
  },
  async updateMentorRound(roundId: string, payload: { result: RoundResult; score: number | null; note: string | null }) {
    return (await api.put<InterviewRound>(`${root}/mentor/rounds/${encodeURIComponent(roundId)}`, payload)).data;
  },
};
