import axios, { type AxiosResponse } from "axios";
import { env } from "@/config/env";
import { useAuthStore } from "@/hooks/use-auth-store";
import type { ApiErrorBody } from "@/interface/auth";

/** Lỗi đã chuẩn hoá từ response lỗi của backend: { code, message, errors }. */
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string | undefined,
    message: string,
    public errors?: unknown,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/** Các endpoint này tự xử lý 401 (sai mật khẩu, vé hết hạn...), không được coi là "phiên hết hạn". */
const PUBLIC_AUTH_PATHS = ["/auth/login", "/auth/sso", "/auth/forgot-password", "/auth/reset-password"];

export const api = axios.create({
  baseURL: env.apiUrl,
  headers: { "Content-Type": "application/json" },
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!axios.isAxiosError(error)) {
      return Promise.reject(error);
    }
    if (!error.response) {
      return Promise.reject(
        new ApiError(0, "NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại."),
      );
    }

    const { status, data } = error.response as AxiosResponse<ApiErrorBody | undefined>;
    const apiError = new ApiError(status, data?.code, data?.message ?? "Đã có lỗi xảy ra. Vui lòng thử lại.", data?.errors);
    const url = error.config?.url ?? "";
    const isPublicAuthCall = PUBLIC_AUTH_PATHS.some((path) => url.startsWith(path));

    if (typeof window !== "undefined") {
      if (status === 401 && !isPublicAuthCall) {
        useAuthStore.getState().clear();
        // Điều hướng cứng (tải lại trang) để xoá sạch trạng thái của phiên đã hết hạn.
        // eslint-disable-next-line @next/next/no-location-assign-relative-destination
        window.location.assign("/login");
      } else if (status === 403 && apiError.code === "MUST_CHANGE_PASSWORD") {
        useAuthStore.getState().setMustChangePassword(true);
        if (window.location.pathname !== "/change-password") {
          // eslint-disable-next-line @next/next/no-location-assign-relative-destination
          window.location.assign("/change-password");
        }
      }
    }
    return Promise.reject(apiError);
  },
);

export function getErrorMessage(error: unknown, fallback = "Đã có lỗi xảy ra. Vui lòng thử lại."): string {
  return error instanceof ApiError ? error.message : fallback;
}
