"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import { staffCompanyKeys, staffJobKeys } from "@/hooks/staff-query-keys";
import type { CreateCompanyPayload, StaffCompanyDetail, StaffCompanyListParams } from "@/interface/company";
import { ApiError } from "@/services/api";
import { staffCompanyService } from "@/services/staff-company.service";

export { staffCompanyKeys } from "@/hooks/staff-query-keys";

const HISTORY_PAGE_SIZE = 20;

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

/** DN-07: lịch sử thao tác của công ty (khoá / mở khoá, duyệt / trả lại / đóng JD), giữ trang cũ khi chuyển trang. */
export function useCompanyHistory(id: string, page: number) {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: staffCompanyKeys.history(userId, id, page),
    queryFn: () => staffCompanyService.history(id, { page, size: HISTORY_PAGE_SIZE }),
    placeholderData: keepPreviousData,
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

/**
 * Khoá / mở khoá công ty xong: ghi chi tiết trả về vào cache, làm mới danh sách công ty, lịch sử của công ty và
 * cache JD của cán bộ (JD của công ty bị khoá hiện "Công ty đang bị khoá"). 409 (đã khoá / đã mở khoá bởi người
 * khác): làm mới cả chi tiết để trang hiện đúng trạng thái. Trang việc làm GET /jobs không có cache TanStack.
 */
function useCompanyLockHandlers(id: string) {
  const queryClient = useQueryClient();
  const userId = useAuthStore((state) => state.user?.id);
  const refreshRelated = () => {
    void queryClient.invalidateQueries({ queryKey: staffCompanyKeys.lists(userId) });
    void queryClient.invalidateQueries({ queryKey: staffCompanyKeys.histories(userId, id) });
    void queryClient.invalidateQueries({ queryKey: staffJobKeys.all(userId) });
  };
  return {
    onSuccess: (detail: StaffCompanyDetail) => {
      queryClient.setQueryData(staffCompanyKeys.detail(userId, id), detail);
      refreshRelated();
    },
    onError: (error: unknown) => {
      if (!(error instanceof ApiError) || error.status !== 409) return;
      void queryClient.invalidateQueries({ queryKey: staffCompanyKeys.detail(userId, id) });
      refreshRelated();
    },
  };
}

export function useLockCompany(id: string) {
  const handlers = useCompanyLockHandlers(id);
  return useMutation({ mutationFn: (reason: string) => staffCompanyService.lock(id, reason), ...handlers });
}

export function useUnlockCompany(id: string) {
  const handlers = useCompanyLockHandlers(id);
  return useMutation({ mutationFn: () => staffCompanyService.unlock(id), ...handlers });
}
