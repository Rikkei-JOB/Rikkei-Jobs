import type { StaffCompanyListParams } from "@/interface/company";
import type { StaffJobListParams } from "@/interface/staff-job";

// Khoá cache của cán bộ, gắn với id người dùng (như use-my-company.ts) để tài khoản khác trong cùng tab không đọc
// nhầm dữ liệu. Để chung một file vì thao tác trên JD và trên công ty làm mới cache của nhau (DN-07).

export const staffCompanyKeys = {
  all: (userId: string | undefined) => ["staff-companies", userId ?? null] as const,
  lists: (userId: string | undefined) => [...staffCompanyKeys.all(userId), "list"] as const,
  list: (userId: string | undefined, params: StaffCompanyListParams) => [...staffCompanyKeys.lists(userId), params] as const,
  detail: (userId: string | undefined, id: string) => [...staffCompanyKeys.all(userId), "detail", id] as const,
  /** Mọi trang lịch sử thao tác của một công ty. */
  histories: (userId: string | undefined, id: string) => [...staffCompanyKeys.all(userId), "history", id] as const,
  history: (userId: string | undefined, id: string, page: number) => [...staffCompanyKeys.histories(userId, id), page] as const,
};

export const staffJobKeys = {
  all: (userId: string | undefined) => ["staff-jobs", userId ?? null] as const,
  list: (userId: string | undefined, params: StaffJobListParams) => [...staffJobKeys.all(userId), "list", params] as const,
  detail: (userId: string | undefined, id: string) => [...staffJobKeys.all(userId), "detail", id] as const,
};
