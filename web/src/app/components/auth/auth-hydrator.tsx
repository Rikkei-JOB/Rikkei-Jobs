"use client"

import { useEffect } from "react"
import { useAuthStore } from "@/hooks/use-auth-store"
import { useQueryClient } from "@tanstack/react-query"

/** Nạp phiên đăng nhập từ localStorage sau khi trang đã mount (đặt một lần trong root layout). */
export function AuthHydrator() {
  const client = useQueryClient()
  useEffect(() => {
    useAuthStore.persist.rehydrate()
    function syncSession() {
      let saved: { token?: string | null; user?: unknown } | null
      try { saved = JSON.parse(localStorage.getItem("rikkei-jobs-auth") ?? "null")?.state ?? null }
      catch { saved = null }
      const current = useAuthStore.getState()
      if ((saved?.token ?? null) === current.token && JSON.stringify(saved?.user ?? null) === JSON.stringify(current.user)) return
      // Drop account-bound reads before mounting UI for the new persisted session.
      client.clear()
      if (saved) useAuthStore.persist.rehydrate()
      else current.clear()
    }
    function onStorage(event: StorageEvent) {
      if (event.storageArea === localStorage && (event.key === "rikkei-jobs-auth" || event.key === null)) syncSession()
    }
    function onVisible() { if (document.visibilityState === "visible") syncSession() }
    window.addEventListener("storage", onStorage)
    window.addEventListener("focus", syncSession)
    document.addEventListener("visibilitychange", onVisible)
    return () => {
      window.removeEventListener("storage", onStorage)
      window.removeEventListener("focus", syncSession)
      document.removeEventListener("visibilitychange", onVisible)
    }
  }, [client])
  return null
}
