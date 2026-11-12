"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import { companyService } from "@/services/company.service";
import type { Company } from "@/interface/company";

// Khoá cache gắn với id người dùng để HR khác trong cùng tab không đọc nhầm công ty của người trước.
export const companyKeys = { mine: (userId: string | undefined) => ["company", "me", userId ?? null] as const };

export function useMyCompany(enabled: boolean) {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({ queryKey: companyKeys.mine(userId), queryFn: companyService.getMine, enabled });
}

export function useUpdateMyCompany() {
  const queryClient = useQueryClient();
  const userId = useAuthStore((state) => state.user?.id);
  return useMutation({
    mutationFn: companyService.updateMine,
    onSuccess: (company: Company) => queryClient.setQueryData(companyKeys.mine(userId), company),
  });
}

export function useUploadMyLogo() {
  const queryClient = useQueryClient();
  const userId = useAuthStore((state) => state.user?.id);
  return useMutation({
    mutationFn: companyService.uploadLogo,
    onSuccess: (company: Company) => queryClient.setQueryData(companyKeys.mine(userId), company),
  });
}
