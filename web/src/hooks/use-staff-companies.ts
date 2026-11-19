"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import type { CreateCompanyPayload, StaffCompanyListParams } from "@/interface/company";
import { staffCompanyService } from "@/services/staff-company.service";

// Khoá cache gắn với id người dùng (như use-my-company.ts) để tài khoản khác trong cùng tab không đọc nhầm dữ liệu.
export const staffCompanyKeys = {
  all: (userId: string | undefined) => ["staff-companies", userId ?? null] as const,
  list: (userId: string | undefined, params: StaffCompanyListParams) =>
    [...staffCompanyKeys.all(userId), "list", params] as const,
  detail: (userId: string | undefined, id: string) => [...staffCompanyKeys.all(userId), "detail", id] as const,
};

export function useStaffCompanies(params: StaffCompanyListParams, enabled = true) {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: staffCompanyKeys.list(userId, params),
    queryFn: () => staffCompanyService.list(params),
    enabled,
  });
}

export function useStaffCompany(id: string, enabled = true) {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: staffCompanyKeys.detail(userId, id),
    queryFn: () => staffCompanyService.detail(id),
    enabled,
  });
}

/** Mutation nào cũng làm mới toàn bộ cache công ty của cán bộ (danh sách và chi tiết). */
function useInvalidateStaffCompanies() {
  const queryClient = useQueryClient();
  const userId = useAuthStore((state) => state.user?.id);
  return () => queryClient.invalidateQueries({ queryKey: staffCompanyKeys.all(userId) });
}

export function useLockHr() {
  const invalidate = useInvalidateStaffCompanies();
  return useMutation({
    mutationFn: ({ userId, reason }: { userId: string; reason: string }) => staffCompanyService.lockHr(userId, reason),
    onSuccess: invalidate,
  });
}

export function useUnlockHr() {
  const invalidate = useInvalidateStaffCompanies();
  return useMutation({
    mutationFn: (userId: string) => staffCompanyService.unlockHr(userId),
    onSuccess: invalidate,
  });
}

export function useCreateCompany() {
  const invalidate = useInvalidateStaffCompanies();
  return useMutation({
    mutationFn: (body: CreateCompanyPayload) => staffCompanyService.createCompany(body),
    onSuccess: invalidate,
  });
}

export function useCreateHr(companyId: string) {
  const invalidate = useInvalidateStaffCompanies();
  return useMutation({
    mutationFn: (body: { email: string; fullName: string }) => staffCompanyService.createHr(companyId, body),
    onSuccess: invalidate,
  });
}
