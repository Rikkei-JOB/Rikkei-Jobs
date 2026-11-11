export type Role = "CANDIDATE" | "HR" | "MENTOR" | "STAFF" | "ADMIN";

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  companyId: string | null;
  mustChangePassword: boolean;
}

/** Body lỗi theo quy ước nhóm: { code, message } và (tuỳ lỗi) errors, ví dụ { minutesRemaining } khi bị khoá. */
export interface ApiErrorBody {
  code?: string;
  message?: string;
  errors?: unknown;
}

export interface SsoResult {
  accessToken: string;
  user: User;
}

export interface LoginResult {
  accessToken: string;
  user: User;
  mustChangePassword: boolean;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface ChangePasswordPayload {
  oldPassword: string;
  newPassword: string;
}

export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
}

/** Người dùng LMS mẫu (khớp mock/data.json), chỉ dùng khi phát triển. */
export interface MockLmsUser {
  lmsUserId: string;
  email: string;
  fullName: string;
  role: Role;
}
