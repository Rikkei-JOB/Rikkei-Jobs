export type SkillSource = "LMS" | "EXAM" | "INTERNSHIP" | "MENTOR" | "SELF";

export type SkillLevel = "BASIC" | "FAIR" | "GOOD" | "EXCELLENT" | null;

export interface Skill {
  id: string;
  code: string;
  name: string;
  group: string;
}

export interface SkillEvidence {
  source: SkillSource;
  title: string;
  score: number;
  recordedAt: string;
  verified: boolean;
}

export interface SkillProfileItem {
  skill: Skill;
  verifiedScore: number | null;
  selfScore: number | null;
  level: SkillLevel;
  evidences: SkillEvidence[];
}

