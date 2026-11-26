"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import { adminService, type AdminUserQuery } from "@/services/admin.service";
import { skillService } from "@/services/skill.service";

const root = (userId: string | undefined) => ["admin", userId ?? null] as const;

export function useAdminUsers(query: AdminUserQuery) {
  const userId = useAuthStore(state => state.user?.id);
  return useQuery({ queryKey: [...root(userId), "users", query], queryFn: () => adminService.users(query), enabled: Boolean(userId), placeholderData: keepPreviousData });
}

export function useAdminUserSummary() {
  const userId = useAuthStore(state => state.user?.id);
  return useQuery({ queryKey: [...root(userId), "users-summary"], queryFn: adminService.userSummary, enabled: Boolean(userId) });
}

/** Khoá / mở khoá xong làm mới danh sách và số đếm. */
export function useToggleUserLock() {
  const client = useQueryClient();
  const userId = useAuthStore(state => state.user?.id);
  return useMutation({
    mutationFn: (action: { id: string; lock: true; reason: string } | { id: string; lock: false }) =>
      action.lock ? adminService.lockUser(action.id, action.reason) : adminService.unlockUser(action.id),
    onSettled: () => client.invalidateQueries({ queryKey: root(userId) }),
  });
}

/** Danh mục kỹ năng (GET /skills công khai); dùng chung khoá cache với bảng việc làm. */
export function useSkillCatalogAdmin() {
  return useQuery({ queryKey: ["job-board", "skills"], queryFn: skillService.getCatalog, staleTime: 60_000 });
}

export function useSaveSkill() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (input: { id?: string; code: string; name: string; group: string }) =>
      input.id ? adminService.updateSkill(input.id, { name: input.name, group: input.group }) : adminService.createSkill(input),
    onSuccess: () => client.invalidateQueries({ queryKey: ["job-board", "skills"] }),
  });
}
