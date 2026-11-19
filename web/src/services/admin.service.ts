import { api } from "@/services/api";
import type {
  AdminEvidence,
  AdminSkill,
  ChangeLog,
  ChangeLogQuery,
  CreateSkillPayload,
  DataReport,
  DataReportStatus,
  DeleteSkillResult,
  EvaluationTemplate,
  EvidenceUpdatePayload,
  PageView,
  ScoreWeight,
  ScoreWeights,
  TemplatePayload,
  UpdateSkillPayload,
} from "@/interface/admin";

/** API trang Quản trị (NT-12), chỉ ADMIN: profile-service qua gateway /api/v1/admin/... */
export const adminService = {
  listSkills: async () => (await api.get<AdminSkill[]>("/admin/skills")).data,
  createSkill: async (payload: CreateSkillPayload) => (await api.post<AdminSkill>("/admin/skills", payload)).data,
  updateSkill: async (id: string, payload: UpdateSkillPayload) =>
    (await api.put<AdminSkill>(`/admin/skills/${id}`, payload)).data,
  deleteSkill: async (id: string, reason?: string) =>
    (await api.delete<DeleteSkillResult>(`/admin/skills/${id}`, { params: { reason: reason || undefined } })).data,

  getWeights: async () => (await api.get<ScoreWeights>("/admin/score-weights")).data,
  saveWeights: async (weights: ScoreWeight[], reason?: string) =>
    (await api.put<ScoreWeights>("/admin/score-weights", { weights, reason: reason || undefined })).data,

  listTemplates: async () => (await api.get<EvaluationTemplate[]>("/admin/evaluation-templates")).data,
  createTemplate: async (payload: TemplatePayload) =>
    (await api.post<EvaluationTemplate>("/admin/evaluation-templates", payload)).data,
  updateTemplate: async (id: string, payload: TemplatePayload) =>
    (await api.put<EvaluationTemplate>(`/admin/evaluation-templates/${id}`, payload)).data,
  deleteTemplate: async (id: string, reason?: string) => {
    await api.delete(`/admin/evaluation-templates/${id}`, { params: { reason: reason || undefined } });
  },

  updateEvidence: async (id: string, payload: EvidenceUpdatePayload) =>
    (await api.put<AdminEvidence>(`/admin/evidences/${id}`, payload)).data,

  listReports: async (status: DataReportStatus | undefined, page: number, size: number) =>
    (await api.get<PageView<DataReport>>("/admin/data-reports", { params: { status, page, size } })).data,
  resolveReport: async (id: string, status: Exclude<DataReportStatus, "OPEN">, resolution: string) =>
    (await api.put<DataReport>(`/admin/data-reports/${id}`, { status, resolution })).data,

  listChangeLogs: async ({ entity, entityId, from, to, page, size }: ChangeLogQuery) =>
    (
      await api.get<PageView<ChangeLog>>("/admin/change-logs", {
        params: { entity, entityId: entityId || undefined, from: from || undefined, to: to || undefined, page, size },
      })
    ).data,
};

/** Ứng viên báo bằng chứng sai và xem kết quả (NT-12): /api/v1/me/data-reports. */
export const dataReportService = {
  mine: async () => (await api.get<DataReport[]>("/me/data-reports")).data,
  create: async (evidenceId: string, content: string) =>
    (await api.post<DataReport>("/me/data-reports", { evidenceId, content })).data,
};
