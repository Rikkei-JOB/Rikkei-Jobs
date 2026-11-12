import { api } from "@/services/api";
import type { SkillProfileItem } from "@/interface/skill";

export const skillService = {
  async getOwnSkills(): Promise<SkillProfileItem[]> {
    const { data } = await api.get<SkillProfileItem[]>("/profiles/me/skills");
    return data;
  },

  async getProfileSkills(userId: string): Promise<SkillProfileItem[]> {
    const { data } = await api.get<SkillProfileItem[]>(`/profiles/${userId}/skills`);
    return data;
  },

  async recomputeSkills(): Promise<SkillProfileItem[]> {
    const { data } = await api.post<SkillProfileItem[]>("/profiles/me/skills/recompute");
    return data;
  },
};

