"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { companyService } from "@/services/company.service";
import type { Company } from "@/interface/company";

export const companyKeys = { mine: ["company", "me"] as const };

export function useMyCompany(enabled: boolean) {
  return useQuery({ queryKey: companyKeys.mine, queryFn: companyService.getMine, enabled });
}

export function useUpdateMyCompany() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: companyService.updateMine,
    onSuccess: (company: Company) => queryClient.setQueryData(companyKeys.mine, company),
  });
}

export function useUploadMyLogo() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: companyService.uploadLogo,
    onSuccess: (company: Company) => queryClient.setQueryData(companyKeys.mine, company),
  });
}
