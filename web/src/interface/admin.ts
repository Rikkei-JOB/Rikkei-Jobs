import type { SkillSource } from "@/interface/skill";

/** NT-12: kiểu dữ liệu của trang Quản trị và báo dữ liệu sai (profile-service). */

export interface PageView<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export interface AdminSkill {
  id: string;
  code: string;
  name: string;
  group: string;
  active: boolean;
  /** Đã có bằng chứng, điểm hoặc ánh xạ nguồn: xoá chỉ tắt. */
  inUse: boolean;
}

export interface CreateSkillPayload {
  code: string;
  name: string;
  group: string;
  reason?: string;
}

export interface UpdateSkillPayload {
  name: string;
  group: string;
  active: boolean;
  reason?: string;
}

export interface DeleteSkillResult {
  id: string;
  deleted: boolean;
  deactivated: boolean;
  message: string;
}

export type WeightSource = "LMS" | "EXAM" | "INTERNSHIP" | "MENTOR";

export interface ScoreWeight {
  source: WeightSource;
  weight: number;
}

export interface ScoreWeights {
  weights: ScoreWeight[];
  total: number;
  updatedAt: string | null;
}

export interface Criterion {
  id?: string;
  name: string;
  description: string | null;
  weight: number;
  maxScore: number;
  sortOrder?: number;
}

export interface EvaluationTemplate {
  id: string;
  name: string;
  description: string | null;
  active: boolean;
  criteria: Criterion[];
  updatedAt: string;
}

export interface TemplatePayload {
  name: string;
  description?: string | null;
  active: boolean;
  criteria: { name: string; description?: string | null; weight: number; maxScore: number }[];
  reason?: string;
}

export interface AdminEvidence {
  id: string;
  userId: string;
  skillId: string;
  skillName: string | null;
  source: SkillSource;
  title: string;
  score: number;
  recordedAt: string;
  verified: boolean;
  fromExternalRecord: boolean;
}

export interface EvidenceUpdatePayload {
  score?: number;
  title?: string;
  reason: string;
}

export type DataReportStatus = "OPEN" | "RESOLVED" | "REJECTED";

export interface DataReport {
  id: string;
  userId: string;
  candidateName: string | null;
  candidateEmail: string | null;
  evidenceId: string | null;
  evidenceTitle: string;
  skillName: string;
  content: string;
  status: DataReportStatus;
  resolution: string | null;
  resolvedAt: string | null;
  createdAt: string;
  /** Bằng chứng hiện tại (chỉ có ở màn ADMIN, null nếu đã bị xoá). */
  evidence: AdminEvidence | null;
}

export type ChangeEntity = "SKILL" | "SCORE_WEIGHT" | "EVALUATION_TEMPLATE" | "PROFILE" | "EVIDENCE" | "DATA_REPORT";

export interface ChangeLog {
  id: string;
  entity: ChangeEntity;
  entityId: string;
  field: string;
  oldValue: string | null;
  newValue: string | null;
  reason: string | null;
  adminId: string;
  createdAt: string;
}

export interface ChangeLogQuery {
  entity?: ChangeEntity;
  entityId?: string;
  from?: string;
  to?: string;
  page: number;
  size: number;
}
