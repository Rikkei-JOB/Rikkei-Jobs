"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminService, dataReportService } from "@/services/admin.service";
import type {
  ChangeLogQuery,
  CreateSkillPayload,
  DataReportStatus,
  EvidenceUpdatePayload,
  ScoreWeight,
  TemplatePayload,
  UpdateSkillPayload,
} from "@/interface/admin";

export const adminKeys = {
  all: ["admin"] as const,
  skills: ["admin", "skills"] as const,
  weights: ["admin", "weights"] as const,
  templates: ["admin", "templates"] as const,
  reports: (status: DataReportStatus | undefined, page: number) => ["admin", "reports", status ?? "ALL", page] as const,
  logs: (query: ChangeLogQuery) => ["admin", "logs", query] as const,
  myReports: ["me", "data-reports"] as const,
};

/** Mọi thay đổi đều sinh nhật ký: làm mới cả nhóm "admin" (bảng Lịch sử thay đổi) và danh mục kỹ năng dùng chung. */
function useInvalidateAdmin() {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: adminKeys.all });
    void queryClient.invalidateQueries({ queryKey: ["skills"] });
  };
}

export function useAdminSkills() {
  return useQuery({ queryKey: adminKeys.skills, queryFn: adminService.listSkills });
}

export function useCreateSkill() {
  const invalidate = useInvalidateAdmin();
  return useMutation({ mutationFn: (payload: CreateSkillPayload) => adminService.createSkill(payload), onSuccess: invalidate });
}

export function useUpdateSkill() {
  const invalidate = useInvalidateAdmin();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateSkillPayload }) => adminService.updateSkill(id, payload),
    onSuccess: invalidate,
  });
}

export function useDeleteSkill() {
  const invalidate = useInvalidateAdmin();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) => adminService.deleteSkill(id, reason),
    onSuccess: invalidate,
  });
}

export function useScoreWeights() {
  return useQuery({ queryKey: adminKeys.weights, queryFn: adminService.getWeights });
}

export function useSaveWeights() {
  const invalidate = useInvalidateAdmin();
  return useMutation({
    mutationFn: ({ weights, reason }: { weights: ScoreWeight[]; reason?: string }) =>
      adminService.saveWeights(weights, reason),
    onSuccess: invalidate,
  });
}

export function useTemplates() {
  return useQuery({ queryKey: adminKeys.templates, queryFn: adminService.listTemplates });
}

export function useSaveTemplate() {
  const invalidate = useInvalidateAdmin();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string | null; payload: TemplatePayload }) =>
      id ? adminService.updateTemplate(id, payload) : adminService.createTemplate(payload),
    onSuccess: invalidate,
  });
}

export function useDeleteTemplate() {
  const invalidate = useInvalidateAdmin();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) => adminService.deleteTemplate(id, reason),
    onSuccess: invalidate,
  });
}

export function useDataReports(status: DataReportStatus | undefined, page: number, size = 10) {
  return useQuery({
    queryKey: adminKeys.reports(status, page),
    queryFn: () => adminService.listReports(status, page, size),
    placeholderData: keepPreviousData,
  });
}

export function useResolveReport() {
  const invalidate = useInvalidateAdmin();
  return useMutation({
    mutationFn: ({ id, status, resolution }: { id: string; status: "RESOLVED" | "REJECTED"; resolution: string }) =>
      adminService.resolveReport(id, status, resolution),
    onSuccess: invalidate,
  });
}

export function useUpdateEvidence() {
  const invalidate = useInvalidateAdmin();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: EvidenceUpdatePayload }) =>
      adminService.updateEvidence(id, payload),
    onSuccess: invalidate,
  });
}

export function useChangeLogs(query: ChangeLogQuery) {
  return useQuery({
    queryKey: adminKeys.logs(query),
    queryFn: () => adminService.listChangeLogs(query),
    placeholderData: keepPreviousData,
  });
}

export function useMyDataReports(enabled = true) {
  return useQuery({ queryKey: adminKeys.myReports, queryFn: dataReportService.mine, enabled });
}

export function useCreateDataReport() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ evidenceId, content }: { evidenceId: string; content: string }) =>
      dataReportService.create(evidenceId, content),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminKeys.myReports }),
  });
}
