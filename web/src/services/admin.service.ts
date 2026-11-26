import { api } from "@/services/api";
import type { Role } from "@/interface/auth";
import type { PageResult } from "@/interface/company";
import type { Skill } from "@/interface/skill";

/** Một tài khoản trong trang quản trị. status LOCKED = bị cán bộ/quản trị khoá. */
export interface AdminUser {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  lmsUserId: string | null;
  companyId: string | null;
  status: "ACTIVE" | "LOCKED";
  lockedReason: string | null;
  lockedAt: string | null;
  createdAt: string;
}

export interface AdminUserSummary { total: number; locked: number; byRole: Record<Role, number> }

export interface AdminUserQuery { q?: string; role?: Role; status?: "ACTIVE" | "LOCKED"; page: number; size?: number }

/** Quản trị tài khoản (identity-service /admin/users) và danh mục kỹ năng (profile-service /admin/skills). */
export const adminService = {
  users: async (query: AdminUserQuery) =>
    (await api.get<PageResult<AdminUser>>("/admin/users", { params: { ...query, q: query.q || undefined } })).data,
  userSummary: async () => (await api.get<AdminUserSummary>("/admin/users/summary")).data,
  lockUser: async (id: string, reason: string) => (await api.patch<AdminUser>(`/admin/users/${id}/lock`, { reason })).data,
  unlockUser: async (id: string) => (await api.patch<AdminUser>(`/admin/users/${id}/unlock`)).data,
  createSkill: async (body: { code: string; name: string; group: string }) => (await api.post<Skill>("/admin/skills", body)).data,
  updateSkill: async (id: string, body: { name: string; group: string }) => (await api.put<Skill>(`/admin/skills/${id}`, body)).data,
};
