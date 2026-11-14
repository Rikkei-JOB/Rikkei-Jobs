import { api } from "@/services/api";
import type { Skill, SkillProfileItem } from "@/interface/skill";

export const skillService = {
  async getCatalog(): Promise<Skill[]> {
    return (await api.get<Skill[]>("/skills")).data;
  },

  async saveSelfSkills(skills: { skillId: string; selfScore: number }[]): Promise<SkillProfileItem[]> {
    return (await api.put<SkillProfileItem[]>("/profiles/me/self-skills", skills)).data;
  },

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

