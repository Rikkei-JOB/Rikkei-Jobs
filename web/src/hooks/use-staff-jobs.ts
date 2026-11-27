"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import { staffCompanyKeys, staffJobKeys } from "@/hooks/staff-query-keys";
import type { StaffJobStatus } from "@/interface/staff-job";
import { staffJobService } from "@/services/staff-job.service";

const root = (userId: string | undefined) => staffJobKeys.all(userId);

export function useStaffJobs(params: { status?: StaffJobStatus; q?: string; companyId?: string; page: number; size?: number }) {
  const userId = useAuthStore(state => state.user?.id);
  return useQuery({
    queryKey: [...root(userId), "list", params],
    queryFn: () => staffJobService.list(params),
    enabled: Boolean(userId),
    placeholderData: keepPreviousData,
  });
}

export function useStaffJobSummary() {
  const userId = useAuthStore(state => state.user?.id);
  return useQuery({ queryKey: [...root(userId), "summary"], queryFn: staffJobService.summary, enabled: Boolean(userId) });
}

export function useStaffJob(id: string) {
  const userId = useAuthStore(state => state.user?.id);
  return useQuery({ queryKey: [...root(userId), "detail", id], queryFn: () => staffJobService.get(id), enabled: Boolean(userId) });
}

/** Thao tác của cán bộ trên một JD: duyệt, trả lại (lý do bắt buộc) hoặc đóng JD đang tuyển (lý do bắt buộc). */
type StaffJobAction = { type: "approve" } | { type: "reject"; reason: string } | { type: "close"; reason: string };

/**
 * Duyệt, trả lại hoặc đóng JD. Thành công thì ghi ngay chi tiết trả về vào cache để màn hình thấy trạng thái mới
 * (không còn cửa sổ nút cũ vẫn bấm được rồi nhận 409). Xong (kể cả lỗi 409 do cán bộ khác vừa xử lý) thì làm mới
 * danh sách, số đếm và chi tiết JD, đồng thời làm mới cache doanh nghiệp vì mỗi thao tác thêm một dòng lịch sử.
 */
export function useReviewStaffJob(id: string) {
  const client = useQueryClient();
  const userId = useAuthStore(state => state.user?.id);
  return useMutation({
    mutationFn: (action: StaffJobAction) =>
      action.type === "approve" ? staffJobService.approve(id)
        : action.type === "reject" ? staffJobService.reject(id, action.reason)
        : staffJobService.close(id, action.reason),
    onSuccess: detail => client.setQueryData([...root(userId), "detail", id], detail),
    onSettled: () => {
      void client.invalidateQueries({ queryKey: root(userId) });
      void client.invalidateQueries({ queryKey: staffCompanyKeys.all(userId) });
    },
  });
}
