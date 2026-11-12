"use client";

import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";

/** Đăng xuất: xoá phiên và cache truy vấn để người đăng nhập sau không thấy dữ liệu của người trước. */
export function useLogout(): () => void {
  const router = useRouter();
  const queryClient = useQueryClient();
  return () => {
    useAuthStore.getState().clear();
    queryClient.clear();
    router.replace("/login");
  };
}
