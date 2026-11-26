"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import { workProfileService, type WorkProfilePayload } from "@/services/work-profile.service";

const key = (userId: string | undefined) => ["work-profile", userId ?? null] as const;

export function useWorkProfile() {
  const userId = useAuthStore(state => state.user?.id);
  return useQuery({ queryKey: key(userId), queryFn: workProfileService.get, enabled: Boolean(userId) });
}

export function useSaveWorkProfile() {
  const client = useQueryClient();
  const userId = useAuthStore(state => state.user?.id);
  return useMutation({
    mutationFn: (payload: WorkProfilePayload) => workProfileService.update(payload),
    onSuccess: profile => client.setQueryData(key(userId), profile),
  });
}
