"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import type { StaffJobStatus } from "@/interface/staff-job";
import { staffJobService } from "@/services/staff-job.service";

const root = (userId: string | undefined) => ["staff-jobs", userId ?? null] as const;

export function useStaffJobs(params: { status?: StaffJobStatus; q?: string; page: number; size?: number }) {
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

/** Duyệt hoặc trả lại; xong (kể cả lỗi 409 do cán bộ khác vừa xử lý) thì làm mới danh sách, số đếm và chi tiết. */
export function useReviewStaffJob(id: string) {
  const client = useQueryClient();
  const userId = useAuthStore(state => state.user?.id);
  return useMutation({
    mutationFn: (action: { type: "approve" } | { type: "reject"; reason: string }) =>
      action.type === "approve" ? staffJobService.approve(id) : staffJobService.reject(id, action.reason),
    onSettled: () => client.invalidateQueries({ queryKey: root(userId) }),
  });
}
