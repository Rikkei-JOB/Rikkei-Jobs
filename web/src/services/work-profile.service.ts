import { api } from "@/services/api";

/**
 * Hồ sơ nghề nghiệp của Mentor, Staff, Admin (profile-service, GET/PUT /profiles/work/me).
 * updatedAt null = chưa lưu lần nào.
 */
export interface WorkProfile {
  title: string | null;
  organization: string | null;
  yearsExperience: number | null;
  phone: string | null;
  linkedinUrl: string | null;
  expertise: string[];
  bio: string | null;
  updatedAt: string | null;
}

export type WorkProfilePayload = Omit<WorkProfile, "updatedAt">;

export const workProfileService = {
  async get(): Promise<WorkProfile> {
    return (await api.get<WorkProfile>("/profiles/work/me")).data;
  },
  async update(payload: WorkProfilePayload): Promise<WorkProfile> {
    return (await api.put<WorkProfile>("/profiles/work/me", payload)).data;
  },
};
