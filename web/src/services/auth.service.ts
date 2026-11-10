import { api } from "@/services/api";
import type {
  ChangePasswordPayload,
  LoginPayload,
  LoginResult,
  ResetPasswordPayload,
  SsoResult,
  User,
} from "@/interface/auth";

/** Các endpoint /api/v1/auth/... Body thành công trả thẳng, không bọc; lỗi có dạng { code, message }. */
export const authService = {
  /** Đổi vé SSO từ LMS lấy token. Vé chỉ dùng được một lần, trong 60 giây. */
  sso: async (ticket: string) => (await api.post<SsoResult>("/auth/sso", { ticket })).data,

  login: async (payload: LoginPayload) => (await api.post<LoginResult>("/auth/login", payload)).data,

  /** Trả token mới: token cũ có thể mang cờ mustChangePassword khiến các service khác trả 403. */
  changePassword: async (payload: ChangePasswordPayload) =>
    (await api.post<{ message: string; accessToken: string }>("/auth/change-password", payload)).data,

  forgotPassword: async (email: string) => {
    await api.post("/auth/forgot-password", { email });
  },

  resetPassword: async (payload: ResetPasswordPayload) => {
    await api.post("/auth/reset-password", payload);
  },

  me: async () => (await api.get<User>("/auth/me")).data,
};
