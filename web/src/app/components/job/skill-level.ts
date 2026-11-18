export type SkillLevelName = "Cơ bản" | "Khá" | "Tốt" | "Xuất sắc";

/** Ranh giới các bậc năng lực (docs/conventions.md): <40 Cơ bản, <70 Khá, <85 Tốt, còn lại Xuất sắc. */
export const SKILL_LEVEL_MARKS = [40, 70, 85] as const;

export function skillLevelLabel(score: number): SkillLevelName {
  if (score < 40) return "Cơ bản";
  if (score < 70) return "Khá";
  if (score < 85) return "Tốt";
  return "Xuất sắc";
}
