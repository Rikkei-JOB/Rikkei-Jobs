export type SkillSource = "LMS" | "EXAM" | "INTERNSHIP" | "MENTOR" | "SELF";

export type SkillLevel = "BASIC" | "FAIR" | "GOOD" | "EXCELLENT" | null;

export interface Skill {
  id: string;
  code: string;
  name: string;
  group: string;
  /** false: kỹ năng đã tắt (NT-12), không chọn mới được nhưng dữ liệu cũ vẫn hiện. */
  active: boolean;
}

export interface SkillEvidence {
  /** Dùng khi ứng viên báo dữ liệu sai (NT-12). */
  id: string;
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

