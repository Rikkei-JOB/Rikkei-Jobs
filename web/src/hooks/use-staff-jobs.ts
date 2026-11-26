"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import { staffCompanyKeys, staffJobKeys } from "@/hooks/staff-query-keys";
import type { StaffJobDetail, StaffJobListParams } from "@/interface/staff-job";
import { ApiError } from "@/services/api";
import { staffJobService } from "@/services/staff-job.service";

export { staffJobKeys } from "@/hooks/staff-query-keys";

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
 * JD của cán bộ, để hàng chờ và số "Chờ duyệt (n)" đúng với DB, cùng lịch sử và chi tiết của công ty sở hữu JD
 * (thao tác được ghi vào lịch sử công ty). Không chờ tải lại xong: màn duyệt cần chọn JD kế tiếp theo hàng chờ
 * cũ trước khi danh sách mới về.
 */
function useInvalidateStaffJobs() {
  const queryClient = useQueryClient();
  const userId = useAuthStore((state) => state.user?.id);
  const invalidate = (jobId: string, companyId: string | undefined) => {
    // Lấy công ty trước khi làm mới: chi tiết JD đang có trong cache (màn duyệt đã tải nó).
    const owner = companyId ?? queryClient.getQueryData<StaffJobDetail>(staffJobKeys.detail(userId, jobId))?.job.company.id;
    void queryClient.invalidateQueries({ queryKey: staffJobKeys.all(userId) });
    if (owner) {
      void queryClient.invalidateQueries({ queryKey: staffCompanyKeys.histories(userId, owner) });
      void queryClient.invalidateQueries({ queryKey: staffCompanyKeys.detail(userId, owner) });
    }
  };
  return {
    onSuccess: (detail: StaffJobDetail, variables: string | { id: string }) =>
      invalidate(typeof variables === "string" ? variables : variables.id, detail.job.company.id),
    onError: (error: unknown, variables: string | { id: string }) => {
      if (error instanceof ApiError && error.status === 409) {
        invalidate(typeof variables === "string" ? variables : variables.id, undefined);
      }
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
