import { api, ApiError } from "@/services/api";

export interface CandidateNotification {
  id: string; type: string; title: string; body: string; href: string; createdAt: string; readAt: string | null;
}
export interface NotificationFeed {
  items: CandidateNotification[]; total: number; page: number; size: number; unreadCount: number;
}
const root = "/recruitment/me/notifications";

export const notificationService = {
  async list(page: number, signal?: AbortSignal): Promise<NotificationFeed> {
    const { data } = await api.get<NotificationFeed>(root, { params: { page, size: 10 }, signal });
    if (!data || !Array.isArray(data.items) || !Number.isFinite(data.unreadCount) || !Number.isFinite(data.total)) {
      throw new ApiError(502, "INVALID_NOTIFICATION_RESPONSE", "Không đọc được dữ liệu thông báo. Vui lòng thử lại.");
    }
    return data;
  },
  async read(id: string) { await api.post(`${root}/${encodeURIComponent(id)}/read`); },
  async readAll() { await api.post(`${root}/read-all`); },
};
