import { api } from "@/services/api";
import type { CandidateProfile, UpdateCandidateProfile } from "@/interface/candidate-profile";

export const candidateProfileService = {
  async getOwn(): Promise<CandidateProfile> {
    return (await api.get<CandidateProfile>("/profiles/me")).data;
  },
  async updateOwn(payload: UpdateCandidateProfile): Promise<CandidateProfile> {
    return (await api.put<CandidateProfile>("/profiles/me", payload)).data;
  },
};
