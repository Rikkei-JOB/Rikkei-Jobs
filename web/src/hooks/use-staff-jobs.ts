"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import type { StaffJobListParams } from "@/interface/staff-job";
import { ApiError } from "@/services/api";
import { staffJobService } from "@/services/staff-job.service";

// Khoá cache gắn với id người dùng (như use-my-company.ts) để tài khoản khác trong cùng tab không đọc nhầm dữ liệu.
export const staffJobKeys = {
  all: (userId: string | undefined) => ["staff-jobs", userId ?? null] as const,
  list: (userId: string | undefined, params: StaffJobListParams) =>
    [...staffJobKeys.all(userId), "list", params] as const,
  detail: (userId: string | undefined, id: string) => [...staffJobKeys.all(userId), "detail", id] as const,
};

/** Danh sách JD của cán bộ (GET /staff/jobs); Task 10 dùng lại cho tab "JD của công ty". */
export function useStaffJobs(params: StaffJobListParams, enabled = true) {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: staffJobKeys.list(userId, params),
    queryFn: () => staffJobService.list(params),
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useStaffJob(id: string | undefined, enabled = true) {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: staffJobKeys.detail(userId, id ?? ""),
    queryFn: () => staffJobService.get(id as string),
    enabled: enabled && !!id,
  });
}

/**
 * Xử lý xong (hoặc 409: JD đã bị người khác xử lý / quá hạn / xung đột) thì làm mới cả danh sách và chi tiết
 * JD của cán bộ, để hàng chờ và số "Chờ duyệt (n)" đúng với DB. Không chờ tải lại xong: màn duyệt cần chọn JD
 * kế tiếp theo hàng chờ cũ trước khi danh sách mới về.
 */
function useInvalidateStaffJobs() {
  const queryClient = useQueryClient();
  const userId = useAuthStore((state) => state.user?.id);
  const invalidate = () => void queryClient.invalidateQueries({ queryKey: staffJobKeys.all(userId) });
  return {
    onSuccess: invalidate,
    onError: (error: unknown) => {
      if (error instanceof ApiError && error.status === 409) invalidate();
    },
  };
}

export function useApproveStaffJob() {
  const handlers = useInvalidateStaffJobs();
  return useMutation({ mutationFn: (id: string) => staffJobService.approve(id), ...handlers });
}

export function useRejectStaffJob() {
  const handlers = useInvalidateStaffJobs();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => staffJobService.reject(id, reason),
    ...handlers,
  });
}

export function useCloseStaffJob() {
  const handlers = useInvalidateStaffJobs();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => staffJobService.close(id, reason),
    ...handlers,
  });
}
