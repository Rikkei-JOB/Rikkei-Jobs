"use client"

import { useEffect } from "react"
import { useAuthStore } from "@/hooks/use-auth-store"

/** Nạp phiên đăng nhập từ localStorage sau khi trang đã mount (đặt một lần trong root layout). */
export function AuthHydrator() {
  useEffect(() => {
    useAuthStore.persist.rehydrate()
  }, [])
  return null
}
