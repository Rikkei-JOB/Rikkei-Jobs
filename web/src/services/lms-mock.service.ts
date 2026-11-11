import { api } from "@/services/api";
import type { MockLmsUser } from "@/interface/auth";

/**
 * Chỉ dùng khi phát triển (NEXT_PUBLIC_LMS_MOCK=true). Danh sách khớp mock/data.json; vé được backend xin từ mock
 * (POST /auth/dev/sso-ticket) vì frontend không được giữ khoá dịch vụ.
 */
export const MOCK_LMS_USERS: MockLmsUser[] = [
  { lmsUserId: "lms-sv-001", email: "candidate1@rikkeijobs.test", fullName: "Nguyễn Văn An", role: "CANDIDATE" },
  { lmsUserId: "lms-sv-002", email: "candidate2@rikkeijobs.test", fullName: "Trần Thị Bình", role: "CANDIDATE" },
  { lmsUserId: "lms-sv-003", email: "candidate3@rikkeijobs.test", fullName: "Lê Minh Châu", role: "CANDIDATE" },
  { lmsUserId: "lms-mt-001", email: "mentor1@rikkeijobs.test", fullName: "Phạm Quốc Dũng", role: "MENTOR" },
  { lmsUserId: "lms-st-001", email: "staff1@rikkeijobs.test", fullName: "Hoàng Thu Hà", role: "STAFF" },
  { lmsUserId: "lms-ad-001", email: "admin1@rikkeijobs.test", fullName: "Đỗ Thanh Hải", role: "ADMIN" },
];

export const lmsMockService = {
  issueTicket: async (lmsUserId: string) =>
    (await api.post<{ ticket: string; issuedAt: string }>("/auth/dev/sso-ticket", { lmsUserId })).data.ticket,
};
