"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import type { RoundResult } from "@/interface/hr-application";
import { hrApplicationService } from "@/services/hr-application.service";

/** Khoá cache gắn id người dùng để mentor khác đăng nhập cùng tab không đọc nhầm dữ liệu. */
export const mentorKeys = { rounds: (userId: string | undefined) => ["mentor", "rounds", userId ?? null] as const };

/**
 * Mọi vòng phỏng vấn mentor được mời (API trả tối đa 100 mỗi trang; một mentor thường ít hơn nhiều).
 * Ba trang Tổng quan, Lịch, Đánh giá dùng chung cache này nên chuyển trang không tải lại.
 */
export function useMentorRounds() {
  const userId = useAuthStore(state => state.user?.id);
  return useQuery({
    queryKey: mentorKeys.rounds(userId),
    queryFn: () => hrApplicationService.listMentorRounds({ page: 1, size: 100 }),
    enabled: Boolean(userId),
    staleTime: 30_000,
  });
}

export function useSaveMentorRound() {
  const client = useQueryClient();
  const userId = useAuthStore(state => state.user?.id);
  return useMutation({
    mutationFn: ({ roundId, ...payload }: { roundId: string; result: RoundResult; score: number | null; note: string | null }) =>
      hrApplicationService.updateMentorRound(roundId, payload),
    // Lưu xong hoặc bị từ chối (409: đơn đã rời bước phỏng vấn) đều tải lại để thấy trạng thái mới nhất.
    onSettled: () => client.invalidateQueries({ queryKey: mentorKeys.rounds(userId) }),
  });
}
