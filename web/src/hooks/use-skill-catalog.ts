"use client";

import { useQuery } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import { skillService } from "@/services/skill.service";

/** Danh mục kỹ năng (profile-service). Ít thay đổi nên giữ cache 5 phút. */
export function useSkillCatalog(enabled = true) {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ["skills", "catalog", userId ?? null],
    queryFn: skillService.getCatalog,
    enabled,
    staleTime: 5 * 60_000,
  });
}
