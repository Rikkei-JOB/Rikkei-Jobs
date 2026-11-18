"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import type { Job, JobPayload, JobStatus } from "@/interface/job";
import { jobService } from "@/services/job.service";

// Khoá cache gắn với id người dùng (như use-my-company.ts) để HR khác trong cùng tab không đọc nhầm JD.
const root = (userId: string | undefined) => ["jobs", userId ?? null] as const;
export const jobKeys = {
  all: root,
  list: (userId: string | undefined, status: JobStatus | undefined, page: number) =>
    [...root(userId), "list", status ?? null, page] as const,
  detail: (userId: string | undefined, id: string) => [...root(userId), "detail", id] as const,
};

export function useMyJobs(status: JobStatus | undefined, page: number, enabled = true) {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: jobKeys.list(userId, status, page),
    queryFn: () => jobService.listMine({ status, page }),
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useJob(id: string | undefined, enabled = true) {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: jobKeys.detail(userId, id ?? ""),
    queryFn: () => jobService.get(id as string),
    enabled: enabled && !!id,
  });
}

/** Cập nhật cache chi tiết và xoá cache danh sách sau khi ghi. */
function useAfterWrite() {
  const queryClient = useQueryClient();
  const userId = useAuthStore((state) => state.user?.id);
  return (job: Job) => {
    queryClient.setQueryData(jobKeys.detail(userId, job.id), job);
    return queryClient.invalidateQueries({ queryKey: [...jobKeys.all(userId), "list"] });
  };
}

/** Có id thì sửa (PUT), không thì tạo mới (POST). */
export function useSaveJob() {
  const afterWrite = useAfterWrite();
  return useMutation({
    mutationFn: ({ id, payload }: { id?: string; payload: JobPayload }) =>
      id ? jobService.update(id, payload) : jobService.create(payload),
    onSuccess: afterWrite,
  });
}

export function useSubmitJob() {
  const afterWrite = useAfterWrite();
  return useMutation({ mutationFn: (id: string) => jobService.submit(id), onSuccess: afterWrite });
}

export function useCloseJob() {
  const afterWrite = useAfterWrite();
  return useMutation({ mutationFn: (id: string) => jobService.close(id), onSuccess: afterWrite });
}
