import { api } from "@/services/api";
import type { MatchScore } from "@/interface/hr-application";

/** Điểm phù hợp giữa ứng viên và JD: /api/v1/jobs/{jobId}/match/... */
export const matchService = {
  forUser: async (jobId: string, userId: string) =>
    (await api.get<MatchScore>(`/jobs/${jobId}/match/${userId}`)).data,

  mine: async (jobId: string) => (await api.get<MatchScore>(`/jobs/${jobId}/match/me`)).data,
};
