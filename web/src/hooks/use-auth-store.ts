import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { User } from "@/interface/auth";

interface AuthState {
  token: string | null;
  user: User | null;
  /** true sau khi đọc xong dữ liệu lưu trong localStorage; trước đó chưa biết đã đăng nhập hay chưa. */
  hasHydrated: boolean;
  setHasHydrated: (value: boolean) => void;
  setSession: (token: string, user: User) => void;
  setUser: (user: User) => void;
  setMustChangePassword: (value: boolean) => void;
  clear: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      hasHydrated: false,
      setHasHydrated: (value) => set({ hasHydrated: value }),
      setSession: (token, user) => set({ token, user }),
      setUser: (user) => set({ user }),
      setMustChangePassword: (value) =>
        set((state) => (state.user ? { user: { ...state.user, mustChangePassword: value } } : state)),
      clear: () => set({ token: null, user: null }),
    }),
    {
      name: "rikkei-jobs-auth",
      partialize: (state) => ({ token: state.token, user: state.user }),
      // Không đọc localStorage khi tạo store (server không có localStorage, sẽ lệch HTML giữa server và client).
      // AuthHydrator gọi rehydrate() sau khi trang đã mount.
      skipHydration: true,
      // Dùng tham số `state` (không dùng biến useAuthStore): với localStorage đồng bộ, callback này chạy
      // ngay trong lúc store đang được tạo, lúc đó useAuthStore chưa tồn tại.
      onRehydrateStorage: () => (state) => {
        state?.setHasHydrated(true);
      },
    },
  ),
);
