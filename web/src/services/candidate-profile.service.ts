import { api } from "@/services/api";
import type { CandidateProfile, ProfileReview, UpdateCandidateProfile } from "@/interface/candidate-profile";

export const candidateProfileService = {
  async publish() {
    return (await api.post<CandidateProfile>("/profiles/me/publish", { consent: true })).data;
  },
  async withdraw() {
    return (await api.post<CandidateProfile>("/profiles/me/withdraw")).data;
  },
  async history() {
    return (await api.get<ProfileReview[]>("/profiles/me/review-history")).data;
  },
  async getOwn(): Promise<CandidateProfile> {
    return (await api.get<CandidateProfile>("/profiles/me")).data;
  },
  async updateOwn(payload: UpdateCandidateProfile): Promise<CandidateProfile> {
    return (await api.put<CandidateProfile>("/profiles/me", payload)).data;
  },
};
