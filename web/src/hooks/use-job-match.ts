"use client";

import { useQuery } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import { matchService } from "@/services/match.service";

// Khoá cache gắn với id người đăng nhập để tài khoản khác trong cùng tab không đọc nhầm dữ liệu.
export const jobMatchKeys = {
  all: (viewerId: string | undefined) => ["job-match", viewerId ?? null] as const,
  forUser: (viewerId: string | undefined, jobId: string, userId: string) =>
    [...jobMatchKeys.all(viewerId), "user", jobId, userId] as const,
  mine: (viewerId: string | undefined, jobId: string) => [...jobMatchKeys.all(viewerId), "me", jobId] as const,
};

/** HR/STAFF xem điểm phù hợp của một ứng viên; chỉ chạy khi có đủ jobId và userId. */
export function useJobMatch(jobId?: string, userId?: string) {
  const viewerId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: jobMatchKeys.forUser(viewerId, jobId ?? "", userId ?? ""),
    queryFn: () => matchService.forUser(jobId!, userId!),
    enabled: Boolean(jobId && userId),
  });
}

/** Ứng viên xem điểm phù hợp của chính mình. */
export function useMyJobMatch(jobId?: string) {
  const viewerId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: jobMatchKeys.mine(viewerId, jobId ?? ""),
    queryFn: () => matchService.mine(jobId!),
    enabled: Boolean(jobId),
  });
}
