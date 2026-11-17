import { ApiError, getErrorMessage } from "@/services/api"

/** Thông báo lỗi cho thao tác trên tài khoản HR; 503 = identity-service không gọi được. */
export function hrErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 503) {
    return "Không kết nối được dịch vụ tài khoản, vui lòng thử lại"
  }
  return getErrorMessage(error)
}
